# SeafoodStore — Fresh Seafood E-commerce

An e-commerce application for a seafood shop: customers browse products sold by weight, place
orders, and pay online through PayOS or on delivery; admins manage stock, orders and discount codes.

University project for HSF302 (Spring Boot). Team of 5, built over roughly 5 weeks.
Java 21 · Spring Boot 4 · Spring Security · Spring Data JPA · SQL Server · Thymeleaf · PayOS · Gemini.

> User-facing text in the app is Vietnamese, since the shop serves Vietnamese customers.
> Code comments, docs and logs are English.

---

## The business problem

Selling seafood differs from ordinary retail in three ways, and those three drive most of the design:

1. **Stock is real, scarce, and indivisible.** If one king crab is left, exactly one customer can
   buy it. Overselling is not a display glitch — it is an order that must be cancelled and a lost customer.
2. **Money arrives from an external system.** PayOS confirms payment through a webhook that is
   delivered asynchronously and **retried until it receives HTTP 200**. Processing it twice credits
   revenue twice.
3. **Products expire.** An abandoned unpaid order still holds stock, so items look sold out on the
   site while they are sitting in the warehouse.

---

## Architecture

```
Browser ──► Controller ──► Service (all business rules) ──► Repository ──► SQL Server
             (Thymeleaf)         │
                                 ├──► PayOS (create link, reconcile)
                                 └──► Gemini (chatbot, timeout + fallback)

PayOS server ──POST──► /api/payments/payos/webhook ──► PaymentService (verify + idempotent)
```

Rule: controllers never call repositories directly. Every business constraint lives in the service
layer so it cannot be bypassed by calling from somewhere else.

| Layer | Package |
|---|---|
| Controllers | `controller/` (`admin/`, `client/`) |
| Business logic | `service/` + `service/impl/` |
| Data access | `repository/`, `specification/` |
| Entities / enums | `entity/`, `enums/` |
| Configuration, jobs | `config/` |

Data model: [`docs/erd.md`](docs/erd.md) · SQL schema: [`docs/schema.sql`](docs/schema.sql)

---

## Three hard problems and how they are solved

### 1. Two customers buying the last crab

**Problem.** The natural way to write this is: read stock → check availability → write new stock.
Two concurrent requests both read `stock = 1`, both conclude "in stock", and both write `0`. Two
orders are created for one crab. This is a **lost update**.

**Solution.** Move the condition into the UPDATE statement itself rather than checking it in Java,
so the check and the write happen in one atomic statement:

```java
// SeafoodProductRepository.java
@Modifying(clearAutomatically = true)
@Query("UPDATE SeafoodProduct s SET s.stockQuantity = s.stockQuantity - :qty " +
       "WHERE s.id = :id AND s.stockQuantity >= :qty AND s.active = true")
int deductStock(@Param("id") Long id, @Param("qty") int qty);
```

The return value is the affected row count: `0` means there was not enough stock, so the order is
rejected. No table lock and no `SELECT ... FOR UPDATE` required.

**Evidence.** [`StockDeductionConcurrencyTest`](src/test/java/com/project/hsf/repository/StockDeductionConcurrencyTest.java)
runs 20 threads buying 5 units at once: **exactly 5 orders succeed, 15 are rejected, stock ends at 0**.
The same file contains a test running the read-then-write version with two threads forced to
interleave via `CountDownLatch`: stock ends at **4 instead of 3** — two units sold but only one
deducted, which is the lost update.

The same mechanism guards coupons (`CouponRepository.claimCoupon`) so one code cannot exceed its
`maxUses` under concurrent requests.

### 2. Anyone could mark their own order as paid

**Problem.** After payment, PayOS redirects the customer to
`/checkout/callback?orderCode=...&status=PAID`. The old code read `status` from that URL and wrote
`PAID` to the database. Typing that URL by hand produced a paid order without paying anything —
URL parameters are controlled by the user, not by PayOS.

**Solution.** Separate "display" from "confirming payment":

- `PAID` is written **only** in [`PayOSWebhookController`](src/main/java/com/project/hsf/controller/PayOSWebhookController.java)
  after `payOS.webhooks().verify(payload)` validates the HMAC signature, or through the
  server-to-server reconciliation path `payOS.paymentRequests().get(orderCode)`.
- The browser redirect is now display-only, and it checks that the order belongs to the logged-in user.
- A valid signature only proves the payload came from PayOS, not that the amount is correct — so the
  webhook `amount` is still compared against the server-side `finalPrice` before payment is accepted.

**What about retried webhooks.** The "not yet paid" condition lives inside the UPDATE statement:

```java
// OrderRepository.java
UPDATE Order o SET o.paymentStatus = PAID, o.orderStatus = CONFIRMED
WHERE o.orderCode = :orderCode AND o.paymentStatus <> PAID AND o.orderStatus <> CANCELLED
```

The first delivery updates 1 row and runs the side effects (write history, store the transaction
reference). Later deliveries update 0 rows, return `ALREADY_PROCESSED` with HTTP 200 so PayOS stops
retrying, and revenue is not credited twice. Check-then-write is not safe here: two concurrent
webhooks would both read `UNPAID` before either one writes.

### 3. Abandoned orders hold stock forever

**Problem.** Stock is deducted at order placement (to prevent overselling), but a PayOS link only
lives 30 minutes. If a customer walks away, those units stay reserved indefinitely.

**Solution.** [`OrderExpiryJob`](src/main/java/com/project/hsf/config/OrderExpiryJob.java) scans
every 5 minutes, cancels expired bank-transfer orders, and **compensates exactly what was deducted**:
stock (read from `order_items`, not from the session, which may already be gone) and coupon usage.

Cancellation goes through `cancelIfStillPending` — also a conditional UPDATE — so if the job and a
customer's cancel click run at the same time, only one of them actually cancels; the other updates
0 rows and skips. Stock is never returned twice.

### Also: the order lifecycle is a state machine

Order status cannot be set freely. `OrderServiceImpl.updateOrderStatus` enforces transitions in the
service layer:

```
PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
   └──────────┴────────────┴───────────┴──────► CANCELLED
```

`DELIVERED` and `CANCELLED` are terminal and cannot be modified. A bank-transfer order that is not
yet `PAID` cannot move to `CONFIRMED`. Every transition is written to `order_status_history` so
disputes can be traced.

---

## Query optimisation

The product list page reads `product.category.name` and `product.images` on **every row**. Both
associations are `LAZY`, so for 30 products Hibernate issued `1 + 30 + 30 = 61` queries — textbook N+1.

The fix uses two different tools for the two kinds of association:

- `@EntityGraph(attributePaths = "category")` — a fetch join for the `@ManyToOne` side.
- `@BatchSize(size = 32)` on the `images` collection — collapses N queries into `ceil(N/32)`
  `IN (...)` queries. A fetch join is deliberately not used for the collection: a product has many
  images, and fetch-joining both `images` and `reviews` would multiply rows (cartesian product).

[`ProductListQueryCountTest`](src/test/java/com/project/hsf/repository/ProductListQueryCountTest.java)
measures this with Hibernate `Statistics`: **61 → 2 queries**, and the count no longer grows with
the number of products.

---

## Product advisor chatbot (Gemini)

A simple RAG setup: fetch relevant documents from the `knowledge_documents` table and put them in
the prompt.

- **Not semantic search.** Retrieval is a `LIKE` keyword lookup — no embeddings, no vector store.
- The customer's question is **split into keywords** before searching. Previously the whole sentence
  was passed straight into `LIKE %...%`, so "do you still have king crab" almost always returned nothing.
- The prompt **forbids the bot from stating concrete prices or stock levels** — both change constantly
  and are not in the knowledge base, and quoting a wrong price to a customer is a real business error.
- There is a 15s `timeout`, `maxRetries` of 1, and a static fallback reply: if Gemini fails or runs
  out of quota, the purchase flow keeps working.

---

## Running it

**Requirements:** JDK 21+, SQL Server, Maven (use the bundled `mvnw`).

```bash
git clone https://github.com/anhnhv2503/GroupProject-HSF302.git
cd GroupProject-HSF302

cp .env.example .env      # fill in DB, PayOS, Gemini, Cloudinary
set -a && source .env && set +a

./mvnw spring-boot:run    # http://localhost:8080
```

An admin account is created on first run (`AppInitConfig`): `admin` / `admin`.

**Running the tests** — no SQL Server needed, tests use in-memory H2:

```bash
./mvnw test
```

**Receiving webhooks during local development:** PayOS needs a public URL. Open a tunnel, register
`https://<tunnel>/api/payments/payos/webhook` in the PayOS dashboard, and set
`APP_BASE_URL=https://<tunnel>` so the redirect does not send customers back to localhost.

---

## Known gaps / next steps

Listed for transparency, not to be ignored:

- **CSRF is disabled** (`SecurityConfig`). Re-enabling it requires adding tokens to every Thymeleaf
  form; not done yet because of the risk of breaking working forms. The webhook endpoint does
  legitimately need to be CSRF-exempt, since PayOS has no session.
- **PayOS keys were once committed** to `application.properties` (commit `ab1f5ee`) and are still in
  git history. They have been removed from the code, but **those keys must be treated as leaked and
  revoked** in the PayOS dashboard.
- Cancelling an already-`PAID` order is blocked in the service layer (a refund must happen first) —
  the refund flow itself is not implemented.
- Tests currently cover the repository layer where data contention happens; there are no
  service/controller-layer tests yet.
- No Docker image and no deployment yet.

---

## My contribution (Nguyen Vi Hung — `itsHungw`)

This is a 5-person team project. The parts I worked on directly:

| Area | Work |
|---|---|
| **Schema bootstrap** | Designed the initial entities and the relationships between tables (`6773342`) |
| **Products / categories** | Admin CRUD, Cloudinary image upload, filter–search–sort with JPA Specification, fixed a numeric overflow on the price column (`b3fc316`, `b275caa`) |
| **Orders** | State machine for the order lifecycle, blocking confirmation of unpaid online orders, writing `order_status_history` (`8171ffb`) |
| **Coupons** | The whole Controller–Service–Repository slice plus business validation in the service layer (`bb88c01`) |
| **Cart / checkout** | Checkout flow, order history, out-of-stock error handling (`01b663b`, `9380b10`) |
| **Auth / RBAC** | Registration and login form validation, 403/404 pages, role-based access (`e36fbb8`) |
| **Payment security** | Fixed the vulnerability of trusting `status` from the redirect URL; PayOS webhook with signature verification and idempotent handling |
| **Quality** | Concurrency test for stock deduction, query-count test for N+1, expired-order cleanup job |

I also reviewed and merged most of the team's pull requests, and resolved conflicts when merging branches.

For accurate credit, work that is **not** mine: the original `deductStock` / `claimCoupon` statements
were written by [@pathwaysuccess1](https://github.com/pathwaysuccess1); the order detail page and the
migration of `orderStatus` to an enum were done by [@ggbb0711](https://github.com/ggbb0711).

Day-by-day work log for the whole team: [`docs/review history/`](docs/review%20history/).

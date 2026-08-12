# Entity Relationship Diagram

16 tables across 4 business areas. The diagram shows keys and the columns that matter for business
logic; full definitions are in [`schema.sql`](schema.sql).

```mermaid
erDiagram
    users ||--o{ user_addresses : "has shipping addresses"
    users ||--o{ orders : "places"
    users ||--o{ wishlists : "saves"
    users ||--o{ product_reviews : "writes"
    users ||--o{ coupon_usages : "redeems"

    categories ||--o{ seafood_products : "classifies"
    seafood_products ||--o{ product_images : "has images"
    seafood_products ||--o{ product_reviews : "is reviewed in"
    seafood_products ||--o{ wishlists : "is saved in"
    seafood_products ||--o{ combo_items : "is part of combo"
    seafood_products ||--o{ knowledge_documents : "has advisor docs"
    combos ||--o{ combo_items : "contains"

    orders ||--o{ order_items : "contains line items"
    orders ||--o{ order_status_history : "status transitions"
    orders ||--|| payments : "one payment record"
    orders }o--o| coupons : "applies"
    seafood_products ||--o{ order_items : "is ordered as"
    coupons ||--o{ coupon_usages : "is redeemed in"

    users {
        bigint id PK
        nvarchar username UK
        nvarchar password "BCrypt"
        nvarchar role "ADMIN / CUSTOMER"
        bit enabled
    }

    seafood_products {
        bigint id PK
        bigint category_id FK
        nvarchar name
        decimal price
        int stock_quantity "deducted via conditional UPDATE"
        int sold_count
        bit active
        nvarchar unit "kg / piece"
        datetime expiry_date
    }

    orders {
        bigint id PK
        bigint customer_id FK
        bigint order_code UK "code sent to PayOS"
        decimal total_price
        decimal discount_amount
        decimal final_price "amount checked against webhook"
        nvarchar order_status "state machine"
        nvarchar payment_status "only a verified webhook sets PAID"
        nvarchar payment_method "COD / BANK_TRANSFER"
        nvarchar coupon_code FK
        datetime created_date "baseline for payment expiry"
    }

    order_items {
        bigint id PK
        bigint order_id FK
        bigint product_id FK
        int quantity "source of truth for restoring stock"
        decimal unit_price "price frozen at order time"
        decimal subtotal
    }

    payments {
        bigint id PK
        bigint order_id FK
        decimal amount
        nvarchar status
        nvarchar transfer_ref "PayOS transaction ref, for reconciliation"
        datetime transferred_at
        nvarchar confirmed_by
    }

    order_status_history {
        bigint id PK
        bigint order_id FK
        nvarchar status
        nvarchar changed_by
        nvarchar note
        datetime changed_at
    }

    coupons {
        bigint id PK
        nvarchar code UK
        nvarchar discount_type "PERCENT / FIXED"
        decimal discount_value
        decimal min_order_value
        int max_uses
        int used_count "incremented via conditional UPDATE"
        datetime valid_until
        bit active
    }
```

## Design decisions worth explaining

**`order_items` stores `unit_price` and `product_name`.** Seafood prices change daily. Joining to
`seafood_products` for the price would make old invoices show today's price — wrong when a customer
disputes a charge. The price is snapshotted at order time.

**`order_items.quantity` is the single source of truth for restoring stock.** When an order is
cancelled, the quantity to return is read from here rather than from the session cart, because the
session may already have been cleared or changed.

**`orders.order_code` is a separate code sent to PayOS**, distinct from `orders.id`. PayOS requires an
integer that we generate; keeping it separate from the primary key avoids exposing the real order
sequence externally.

**`orders.final_price` is stored, not recomputed.** The PayOS webhook reports the amount paid, so a
fixed server-side figure is needed to compare against — it cannot be recalculated from the cart.

**`order_status_history` is a separate, append-only table.** We need to know who changed the status,
when, and why; a single `order_status` column on `orders` cannot hold history.

**`payments.transfer_ref` holds the PayOS transaction reference.** When reconciling against a bank
statement, this is the link between a statement line and an order in the system.

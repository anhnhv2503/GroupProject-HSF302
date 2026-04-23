# FE Restructuring to Atomic Design

Chuyển đổi cấu trúc Frontend từ copy-paste `<head>`, `<header>`, `<footer>` trên mỗi trang → sang kiến trúc **Atomic Design** với Layout Decorator pattern của Thymeleaf.

## Current State Analysis

**Vấn đề hiện tại:**
- 10 trang HTML đang copy-paste `<head>`, import header/footer fragment thủ công
- Design tokens (colors, fonts, Tailwind config) bị duplicate trên mỗi file
- Không có master layout → thay đổi UI phải sửa TỪNG file
- Có `customer-layout.html` nhưng **KHÔNG ai dùng** (0 references)

**Cấu trúc hiện tại:**
```
templates/
├── fragments/
│   ├── index-header.html    ← header fragment (th:fragment="header")
│   └── index-footer.html    ← footer fragment (th:fragment="footer")
├── layout/
│   ├── customer-layout.html ← UNUSED layout
│   └── fragments/           ← profile fragments (unrelated)
├── index.html               ← Full HTML page, self-contained
├── user/                    ← product-list, product-detail, wishlist, profile, address
├── cart/cart.html
├── checkout/checkout.html
├── order/orders.html, order-detail.html
└── admin/                   ← separate system
```

**10 pages** reference `fragments/index-header` và `fragments/index-footer` trực tiếp.

---

## Proposed Changes

### Task 1: Tạo cấu trúc thư mục Atomic Design

Tạo các thư mục mới theo chuẩn Atomic:

```
templates/
├── fragments/
│   ├── layout/              ← [NEW] Layout-level fragments
│   │   ├── head.html        ← [NEW] Centralized design tokens
│   │   ├── header.html      ← [MOVE] from fragments/index-header.html
│   │   └── footer.html      ← [MOVE] from fragments/index-footer.html
│   ├── components/          ← [NEW] Reusable UI components
│   │   └── product-card.html ← [NEW] Extracted product card
│   ├── index-header.html    ← KEEP (backward compat, sẽ redirect hoặc xóa sau)
│   └── index-footer.html    ← KEEP (backward compat)
├── layout/
│   └── main.html            ← [NEW] Master layout template
```

> [!IMPORTANT]
> Vì 10 file hiện tại đang reference `fragments/index-header` và `fragments/index-footer`, tôi sẽ **KHÔNG xóa** các file cũ, chỉ **copy** content sang vị trí mới. Các file cũ vẫn giữ nguyên để các trang chưa migrate vẫn hoạt động. Bạn có muốn tôi xóa luôn file cũ không?

---

### Task 2: Centralized Design Tokens — `fragments/layout/head.html`

#### [NEW] [head.html](file:///c:/Projects/HSF_SeaFood/GroupProject-HSF302/src/main/resources/templates/fragments/layout/head.html)

Tập trung tất cả:
- Tailwind CDN + Config (colors `#D35400`, font `Playfair Display`, full Material Design 3 color tokens)
- Google Fonts (Be Vietnam Pro, Playfair Display, Roboto)
- Material Symbols, Bootstrap Icons
- Global CSS (glass-effect, premium-badge, focus styles)

Đây sẽ là **Single Source of Truth** cho mọi design token.

---

### Task 3: Header & Footer Fragments

#### [NEW] [header.html](file:///c:/Projects/HSF_SeaFood/GroupProject-HSF302/src/main/resources/templates/fragments/layout/header.html)

Copy content từ `fragments/index-header.html` → `fragments/layout/header.html`. Giữ nguyên `th:fragment="header"`.

#### [NEW] [footer.html](file:///c:/Projects/HSF_SeaFood/GroupProject-HSF302/src/main/resources/templates/fragments/layout/footer.html)

Copy content từ `fragments/index-footer.html` → `fragments/layout/footer.html`. Giữ nguyên `th:fragment="footer"`.

---

### Task 4: Master Layout — `layout/main.html`

#### [NEW] [main.html](file:///c:/Projects/HSF_SeaFood/GroupProject-HSF302/src/main/resources/templates/layout/main.html)

Master template sử dụng `layout:fragment="content"`:

```html
<!DOCTYPE html>
<html xmlns:th="..." xmlns:layout="..." xmlns:sec="...">
<head>
    <!-- Include centralized tokens -->
    <th:block th:replace="~{fragments/layout/head :: head}"></th:block>
    <title layout:title-pattern="$CONTENT_TITLE - The Culinary Curator">The Culinary Curator</title>
</head>
<body class="bg-surface text-on-surface">
    <header th:replace="~{fragments/layout/header :: header}"></header>
    <main class="relative z-0 pt-32">
        <div layout:fragment="content"><!-- child pages inject here --></div>
    </main>
    <div th:replace="~{fragments/layout/footer :: footer}"></div>
    <div th:replace="~{chatbot/chatbot :: widget}"></div>
    <!-- Shared scripts block -->
    <th:block layout:fragment="scripts"></th:block>
</body>
</html>
```

Các trang con chỉ cần:
```html
<html layout:decorate="~{layout/main}">
<head><title>Trang chủ</title></head>
<body>
    <div layout:fragment="content">
        <!-- Page-specific content only -->
    </div>
    <th:block layout:fragment="scripts">
        <!-- Page-specific scripts -->
    </th:block>
</body>
</html>
```

---

### Task 5: Product Card Component

#### [NEW] [product-card.html](file:///c:/Projects/HSF_SeaFood/GroupProject-HSF302/src/main/resources/templates/fragments/components/product-card.html)

Extract product card UI từ `index.html` (lines 217-270) thành `th:fragment="card(product, canAddToCart)"`:
- Sử dụng color token `#D35400` (mapped to brand colors)
- Thêm `aria-label` cho accessibility
- Reusable cho cả homepage và product-list page

---

### Task 6: Refactor `index.html`

#### [MODIFY] [index.html](file:///c:/Projects/HSF_SeaFood/GroupProject-HSF302/src/main/resources/templates/index.html)

- Xóa `<!DOCTYPE>`, `<html>`, `<head>`, `<body>` tags tự chứa
- Thêm `layout:decorate="~{layout/main}"`
- Wrap toàn bộ main content trong `layout:fragment="content"`
- Di chuyển scripts vào `layout:fragment="scripts"`
- Sử dụng `product-card` component thay vì inline HTML

---

## Open Questions

> [!IMPORTANT]
> **Q1:** Bạn có muốn tôi xóa file cũ (`fragments/index-header.html`, `fragments/index-footer.html`) hay giữ lại để các trang chưa migrate vẫn chạy được?

> [!IMPORTANT]  
> **Q2:** Bạn chỉ muốn refactor `index.html` trước, hay muốn tôi migrate **tất cả 10 trang** sang dùng `layout/main.html` luôn trong lần này?

> [!NOTE]
> **Q3:** File `layout/customer-layout.html` hiện không ai sử dụng. Có xóa đi không?

---

## Verification Plan

### Automated Tests
- Chạy `mvn spring-boot:run` để verify ứng dụng khởi động không lỗi
- Mở browser tại `localhost:8080` kiểm tra trang chủ render đúng

### Manual Verification
- Kiểm tra header, footer hiển thị đúng
- Kiểm tra product cards render đúng data
- Kiểm tra responsive layout
- Kiểm tra các trang chưa migrate vẫn hoạt động (nếu giữ file cũ)

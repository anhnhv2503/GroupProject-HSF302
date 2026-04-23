USE SeafoodStoreDB;
GO

-- ============================================================
-- [1] USERS
-- ============================================================
CREATE TABLE users
(
    id                 BIGINT IDENTITY (1,1) PRIMARY KEY,
    username           NVARCHAR(50)  NOT NULL UNIQUE,
    email              NVARCHAR(150) NOT NULL UNIQUE,
    password           NVARCHAR(255) NOT NULL,
    full_name          NVARCHAR(150),
    phone              NVARCHAR(20),
    -- ⚠️ address đã xóa — dùng bảng user_addresses (multi-address) thay thế
    role               NVARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER',
    enabled            BIT           NOT NULL DEFAULT 1,
    is_comment_blocked BIT           NOT NULL DEFAULT 0,
    created_date       DATETIME2              DEFAULT GETDATE(),
    updated_date       DATETIME2              DEFAULT GETDATE(),
    CONSTRAINT chk_user_role CHECK (role IN ('CUSTOMER', 'ADMIN'))
);

-- ============================================================
-- [1b] CATEGORIES  (chuẩn hoá category — admin chọn dropdown, không tự gõ)
-- ============================================================
CREATE TABLE categories
(
    id           BIGINT IDENTITY (1,1) PRIMARY KEY,
    name         NVARCHAR(100) NOT NULL UNIQUE,
    description  NVARCHAR(500),
    active       BIT           NOT NULL DEFAULT 1,
    created_date DATETIME2              DEFAULT GETDATE(),
    updated_date DATETIME2              DEFAULT GETDATE()
);

-- ============================================================
-- [2] SEAFOOD PRODUCTS
-- ============================================================
-- soft_delete chiến lược: active = 0 → service layer bắt buộc JOIN AND p.active = 1
-- ⚠️ Khi deactivate product: cart_items + wishlists có thể vẫn giữ item đó
-- Chiến lược chọn: LUÔN filter active=1 khi query wishlist/cart trong service
-- Không dùng scheduled job (tránh phức tạp), filter ở CartServiceImpl + WishlistServiceImpl
-- ⚠️ RACE CONDITION stock_quantity: KHÔNG SELECT rồi UPDATE riêng — dùng atomic UPDATE:
--   UPDATE seafood_products
--   SET stock_quantity = stock_quantity - ?
--   WHERE id = ? AND stock_quantity >= ? AND active = 1
--   → affected rows = 0 → hết hàng, rollback toàn bộ transaction trong OrderServiceImpl
CREATE TABLE seafood_products
(
    id               BIGINT IDENTITY (1,1) PRIMARY KEY,
    category_id      BIGINT         NOT NULL,
    name             NVARCHAR(200)  NOT NULL,
    description      NVARCHAR(MAX),
    price            DECIMAL(10, 2) NOT NULL CHECK (price > 0),
    stock_quantity   INT            NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    freshness_status NVARCHAR(20)   NOT NULL DEFAULT 'FRESH',
    sold_count       INT            NOT NULL DEFAULT 0 CHECK (sold_count >= 0),
    imported_date    DATETIME2,
    expiry_date      DATETIME2,
    imported_from    NVARCHAR(200),
    active           BIT            NOT NULL DEFAULT 1,
    unit             NVARCHAR(20),
    created_date     DATETIME2               DEFAULT GETDATE(),
    updated_date     DATETIME2               DEFAULT GETDATE(),
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT chk_freshness CHECK (freshness_status IN ('FRESH', 'FROZEN', 'PROCESSED')),
    CONSTRAINT chk_product_dates CHECK (expiry_date IS NULL OR imported_date IS NULL OR imported_date <= expiry_date)
);

-- ============================================================
-- [2b] PRODUCT IMAGES
-- ============================================================
CREATE TABLE product_images
(
    id           BIGINT IDENTITY (1,1) PRIMARY KEY,
    product_id   BIGINT        NOT NULL,
    image_url    NVARCHAR(500) NOT NULL,
    public_id    NVARCHAR(200),
    is_primary   BIT           NOT NULL DEFAULT 0,
    created_date DATETIME2              DEFAULT GETDATE(),
    CONSTRAINT fk_image_product FOREIGN KEY (product_id) REFERENCES seafood_products (id) ON DELETE CASCADE
);

-- ============================================================
-- [2c] COMBOS (bundle nhiều sản phẩm)
-- ============================================================
-- ⚠️ CartServiceImpl: khi thêm combo vào giỏ, kiểm tra:
--   1. combo.active = 1 AND (valid_until IS NULL OR valid_until > GETDATE())
--   2. combo.stock_quantity >= 1
--   3. Từng product trong combo_items vẫn active = 1 AND stock_quantity >= quantity
CREATE TABLE combos
(
    id             BIGINT IDENTITY (1,1) PRIMARY KEY,
    name           NVARCHAR(200)  NOT NULL,
    description    NVARCHAR(MAX),
    combo_price    DECIMAL(10, 2) NOT NULL CHECK (combo_price > 0),
    stock_quantity INT            NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    sold_count     INT            NOT NULL DEFAULT 0 CHECK (sold_count >= 0),
    image_url      NVARCHAR(500),
    active         BIT            NOT NULL DEFAULT 1,
    valid_from     DATETIME2      NOT NULL DEFAULT GETDATE(),
    valid_until    DATETIME2, -- nullable = không giới hạn
    created_date   DATETIME2               DEFAULT GETDATE(),
    updated_date   DATETIME2               DEFAULT GETDATE(),
    CONSTRAINT chk_combo_dates CHECK (valid_until IS NULL OR valid_from < valid_until)
);
-- ⚠️ RACE CONDITION stock_quantity: Dùng atomic UPDATE giống seafood_products

CREATE TABLE combo_items
(
    id         BIGINT IDENTITY (1,1) PRIMARY KEY,
    combo_id   BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity   INT    NOT NULL CHECK (quantity > 0),
    CONSTRAINT fk_comboitems_combo FOREIGN KEY (combo_id) REFERENCES combos (id) ON DELETE CASCADE,
    CONSTRAINT fk_comboitems_product FOREIGN KEY (product_id) REFERENCES seafood_products (id) ON DELETE NO ACTION, -- không dùng CASCADE DELETE cho combo_items → product (tránh xóa SP làm vỡ combo)
    CONSTRAINT uq_combo_product UNIQUE (combo_id, product_id)
);

-- ============================================================
-- [3] CARTS AND CART ITEMS  ← ĐÃ XÓA theo đề xuất thầy
-- ============================================================
-- Giỏ hàng được quản lý hoàn toàn ở tầng Service / Session
-- (e.g. HttpSession hoặc Redis) — không cần persist vào DB
-- Lý do: đơn giản hóa schema, không phát sinh orphan cart data

-- ============================================================
-- [4] COUPONS  (v2 — mới)
-- ============================================================
-- ⚠️ RACE CONDITION: KHÔNG dùng SELECT used_count rồi so sánh → UPDATE riêng
-- Dùng atomic UPDATE trong CouponServiceImpl:
--   UPDATE coupons SET used_count = used_count + 1
--   WHERE code = ? AND used_count < max_uses AND active = 1 AND valid_until > GETDATE()
--   → Kiểm tra affected rows = 1; nếu = 0 thì coupon đã hết lượt (thread-safe)
CREATE TABLE coupons
(
    id              BIGINT IDENTITY (1,1) PRIMARY KEY,
    code            NVARCHAR(50)   NOT NULL UNIQUE,
    discount_type   NVARCHAR(10)   NOT NULL DEFAULT 'PERCENT', -- PERCENT | FIXED
    discount_value  DECIMAL(10, 2) NOT NULL,
    min_order_value DECIMAL(10, 2) NOT NULL DEFAULT 0 CHECK (min_order_value >= 0),
    max_uses        INT            NOT NULL DEFAULT 100 CHECK (max_uses > 0),
    used_count      INT            NOT NULL DEFAULT 0,
    valid_from      DATETIME2      NOT NULL DEFAULT GETDATE(),
    valid_until     DATETIME2      NOT NULL,
    active          BIT            NOT NULL DEFAULT 1,
    created_date    DATETIME2               DEFAULT GETDATE(),
    updated_date    DATETIME2               DEFAULT GETDATE(),
    CONSTRAINT chk_discount_value CHECK (
        (discount_type = 'PERCENT' AND discount_value BETWEEN 0.01 AND 100)
            OR
        (discount_type = 'FIXED' AND discount_value > 0)
        ),
    CONSTRAINT chk_coupon_dates CHECK (valid_from < valid_until)
);

-- ============================================================
-- [4b] COUPON USAGES  (v2 — track ai đã dùng coupon nào)
-- ============================================================
-- ⚠️ TRANSACTION SAFETY: toàn bộ flow nằm trong 1 @Transactional duy nhất:
--   atomic UPDATE coupon → INSERT order → INSERT order_items → INSERT coupon_usages
--   Nếu bất kỳ bước nào fail → rollback toàn bộ, không bị orphan used_count
-- order_id: KHÔNG có FK — tại vì insert sau order nhưng trong cùng transaction
CREATE TABLE coupon_usages
(
    id          BIGINT IDENTITY (1,1) PRIMARY KEY,
    coupon_code NVARCHAR(50) NOT NULL,
    user_id     BIGINT       NOT NULL,
    order_id    BIGINT, -- tham chiếu order, không FK (tham chiếu trong cùng transaction)
    used_at     DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT fk_usage_coupon FOREIGN KEY (coupon_code) REFERENCES coupons (code),
    CONSTRAINT fk_usage_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_coupon_user UNIQUE (coupon_code, user_id)
);

-- ============================================================
-- [5] ORDERS  (v2 — coupon + discount + payment + shipping)
-- ============================================================
-- final_price là COMPUTED COLUMN (PERSISTED) — tự động tính lại khi 3 cột kia thay đổi
-- KHÔNG bao giờ lệch, có thể index, không cần SET trong INSERT/UPDATE
-- ⚠️ discount_amount service-layer: Math.min(calculatedDiscount, totalPrice)
--    tránh trường hợp coupon FIXED $20 áp vào đơn $15 → final_price âm
CREATE TABLE orders
(
    id               BIGINT IDENTITY (1,1) PRIMARY KEY,
    customer_id      BIGINT         NOT NULL,
    total_price      DECIMAL(10, 2) NOT NULL CHECK (total_price > 0), -- không đặt đơn rỗng
    discount_amount  DECIMAL(10, 2) NOT NULL DEFAULT 0,
    shipping_fee     DECIMAL(10, 2) NOT NULL DEFAULT 0,
    -- ⚠️ JPA: @Column(name = "final_price", insertable = false, updatable = false)
    final_price      AS (total_price - discount_amount + shipping_fee) PERSISTED,
    order_status     NVARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    payment_method   NVARCHAR(30)   NOT NULL DEFAULT 'COD',
    payment_status   NVARCHAR(20)   NOT NULL DEFAULT 'UNPAID',
    -- COD:           UNPAID → PAID (khi giao hàng xong)
    -- BANK_TRANSFER: UNPAID → PAID (khi admin xác nhận)
    shipping_address NVARCHAR(500)  NOT NULL,
    coupon_code      NVARCHAR(50),
    notes            NVARCHAR(1000),
    created_date     DATETIME2               DEFAULT GETDATE(),
    updated_date     DATETIME2               DEFAULT GETDATE(),
    CONSTRAINT fk_order_customer FOREIGN KEY (customer_id) REFERENCES users (id),
    CONSTRAINT fk_order_coupon FOREIGN KEY (coupon_code) REFERENCES coupons (code)
        ON UPDATE NO ACTION,
    CONSTRAINT chk_order_status CHECK (order_status IN
                                       ('PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT chk_payment_method CHECK (payment_method IN ('COD', 'BANK_TRANSFER')),
    CONSTRAINT chk_payment_status CHECK (payment_status IN ('UNPAID', 'PAID', 'REFUNDED')),
    CONSTRAINT chk_discount_not_exceed CHECK (discount_amount <= total_price),
    CONSTRAINT chk_fees_positive CHECK (shipping_fee >= 0 AND discount_amount >= 0)
);

-- ============================================================
-- [6] ORDER ITEMS  (v2 — thêm snapshot name + image)
-- ============================================================
-- IMMUTABLE: không UPDATE order_items sau khi INSERT
-- subtotal là COMPUTED COLUMN: luôn = quantity × unit_price, không bao giờ lệch
CREATE TABLE order_items
(
    id                BIGINT IDENTITY (1,1) PRIMARY KEY,
    order_id          BIGINT         NOT NULL,
    product_id        BIGINT,                               -- Nullable cho trường hợp order dòng này là một combo
    combo_id          BIGINT,                               -- Nullable cho trường hợp order dòng này là SP lẻ, FK → combos ON UPDATE NO ACTION
    product_name      NVARCHAR(200),                        -- snapshot tên SP khi đặt hàng
    combo_name        NVARCHAR(200),                        -- snapshot tên combo lúc đặt hàng
    -- product_image_url: snapshot PRIMARY image lúc đặt hàng
    -- Lấy từ: SELECT image_url FROM product_images WHERE product_id = ? AND is_primary = 1 (Với SP lẻ)
    -- Hoặc:   SELECT image_url FROM combos WHERE id = ? (Với combo)
    product_image_url NVARCHAR(500),
    quantity          INT            NOT NULL CHECK (quantity > 0),
    unit_price        DECIMAL(10, 2) NOT NULL CHECK (unit_price > 0),
    -- ⚠️ JPA: @Column(name = "subtotal", insertable = false, updatable = false)
    subtotal          AS (quantity * unit_price) PERSISTED, -- không bao giờ sai
    created_date      DATETIME2 DEFAULT GETDATE(),
    -- KHÔNG có updated_date: order_items là immutable record
    CONSTRAINT fk_orderitem_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_orderitem_product FOREIGN KEY (product_id) REFERENCES seafood_products (id),
    CONSTRAINT fk_orderitem_combo FOREIGN KEY (combo_id) REFERENCES combos (id) ON UPDATE NO ACTION,
    CONSTRAINT chk_orderitem_type CHECK ((combo_id IS NULL AND product_id IS NOT NULL) OR
                                         (combo_id IS NOT NULL AND product_id IS NULL))
);

-- ============================================================
-- [7] ORDER STATUS HISTORY  (v2 — mới · tracking timeline)
-- ============================================================
CREATE TABLE order_status_history
(
    id         BIGINT IDENTITY (1,1) PRIMARY KEY,
    order_id   BIGINT       NOT NULL,
    status     NVARCHAR(20) NOT NULL,
    note       NVARCHAR(500),
    changed_by NVARCHAR(50), -- username của người thay đổi (admin | system)
    changed_at DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT fk_history_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT chk_history_status CHECK (status IN
                                         ('PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED'))
);

-- ============================================================
-- [12] PAYMENTS  (lịch sử giao dịch thanh toán)
-- ============================================================
-- Mỗi order có thể có nhiều payment record (ví dụ: thanh toán lần 1 thất bại,
-- lần 2 thành công) — chỉ 1 record có status = 'SUCCESS' là hợp lệ
--
-- COD flow:
--   1. User đặt hàng → INSERT payments(status='PENDING', payment_method='COD')
--   2. Shipper giao hàng xong → UPDATE payments SET status='SUCCESS', confirmed_by='system'
--                              → UPDATE orders SET payment_status='PAID', order_status='DELIVERED'
--
-- BANK_TRANSFER flow:
--   1. User đặt hàng → INSERT payments(status='PENDING', payment_method='BANK_TRANSFER')
--   2. User upload ảnh → UPDATE payments SET transfer_image=?, transferred_at=?
--   3. Admin xác nhận → UPDATE payments SET status='SUCCESS', confirmed_by=?, confirmed_at=?
--                      → UPDATE orders SET payment_status='PAID'
CREATE TABLE payments
(
    id             BIGINT IDENTITY (1,1) PRIMARY KEY,
    order_id       BIGINT         NOT NULL,
    payment_method NVARCHAR(30)   NOT NULL, -- COD | BANK_TRANSFER
    amount         DECIMAL(10, 2) NOT NULL CHECK (amount > 0),
    status         NVARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    -- PENDING | SUCCESS | FAILED | REFUNDED

    -- Thông tin bank transfer (nullable — chỉ dùng khi payment_method = BANK_TRANSFER)
    bank_account   NVARCHAR(50),            -- số tài khoản khách chuyển đến
    transfer_ref   NVARCHAR(100),           -- mã giao dịch / nội dung chuyển khoản
    transfer_image NVARCHAR(500),           -- URL ảnh chụp biên lai (upload lên server)
    transferred_at DATETIME2,               -- thời điểm khách khai báo đã chuyển

    -- Audit
    confirmed_by   NVARCHAR(50),            -- username admin xác nhận (hoặc 'system' với COD)
    confirmed_at   DATETIME2,               -- thời điểm admin xác nhận
    note           NVARCHAR(500),
    created_date   DATETIME2               DEFAULT GETDATE(),
    updated_date   DATETIME2               DEFAULT GETDATE(),

    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT chk_pay_method CHECK (payment_method IN ('COD', 'BANK_TRANSFER')),
    CONSTRAINT chk_pay_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED'))
);

CREATE INDEX idx_payments_order ON payments (order_id);
CREATE INDEX idx_payments_status ON payments (status);

-- ============================================================
-- [8] PRODUCT REVIEWS  (v2 — mới · rating & comment · moderated)
-- ============================================================
-- ⚠️ NGHIỆP VỤ: Chỉ cho phép review khi user có ORDER status=DELIVERED chứa product_id này
-- Validation thực hiện ở ReviewServiceImpl.submitReview() — không enforce tại DB
-- is_visible: admin set = 0 để ẩn review spam/vi phạm mà không xóa (giữ audit trail)
CREATE TABLE product_reviews
(
    id           BIGINT IDENTITY (1,1) PRIMARY KEY,
    product_id   BIGINT  NOT NULL,
    user_id      BIGINT  NOT NULL,
    rating       TINYINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment      NVARCHAR(1000),
    is_visible   BIT     NOT NULL DEFAULT 1, -- 1=hiển thị, 0=admin ẩn (không xóa)
    created_date DATETIME2        DEFAULT GETDATE(),
    updated_date DATETIME2        DEFAULT GETDATE(),
    CONSTRAINT fk_review_product FOREIGN KEY (product_id)
        REFERENCES seafood_products (id) ON DELETE CASCADE,
    CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_one_review UNIQUE (product_id, user_id)
);

-- ============================================================
-- [9] WISHLISTS  (v2 — mới · yêu thích sản phẩm)
-- ============================================================
CREATE TABLE wishlists
(
    id         BIGINT IDENTITY (1,1) PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    added_date DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT fk_wish_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_wish_product FOREIGN KEY (product_id)
        REFERENCES seafood_products (id),
    CONSTRAINT uq_wishlist UNIQUE (user_id, product_id)
);

-- ============================================================
-- [10] USER ADDRESSES  (v2 — mới · nhiều địa chỉ giao hàng · Việt Nam)
-- ============================================================
-- ⚠️ is_default: DB KHÔNG enforce unique — service đảm bảo:
--   khi set 1 địa chỉ làm default → UPDATE tất cả địa chỉ khác của user về is_default=0
--   thực hiện trong cùng 1 @Transactional trong AddressServiceImpl
-- ⚠️ Checkout snapshot: ghép đầy đủ address_line + ward + city
--   trước khi lưu vào orders.shipping_address (1 string phẳng)
CREATE TABLE user_addresses
(
    id           BIGINT IDENTITY (1,1) PRIMARY KEY,
    user_id      BIGINT        NOT NULL,
    phone        NVARCHAR(20)  NOT NULL,
    address_line NVARCHAR(500) NOT NULL, -- số nhà, tên đường
    ward         NVARCHAR(100),          -- Phường/Xã
    city         NVARCHAR(100),          -- Tỉnh/Thành phố
    is_default   BIT           NOT NULL DEFAULT 0,
    created_date DATETIME2              DEFAULT GETDATE(),
    updated_date DATETIME2              DEFAULT GETDATE(),
    CONSTRAINT fk_address_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);

-- ============================================================
-- [11] KNOWLEDGE DOCUMENTS  (RAG chatbot — giữ nguyên từ v1)
-- ============================================================
-- product_id (nullable): link document với sản phẩm cụ thể cho RAG chatbot
-- NULL = document chung (chính sách, FAQ, hướng dẫn)
-- NOT NULL = document mô tả sản phẩm → chatbot trả lời chính xác hơn về SP đó
CREATE TABLE knowledge_documents
(
    id           BIGINT IDENTITY (1,1) PRIMARY KEY,
    product_id   BIGINT,                                    -- nullable: NULL=chung, có giá trị=SP cụ thể
    title        NVARCHAR(200) NOT NULL,
    content      NVARCHAR(MAX) NOT NULL,
    category     NVARCHAR(100),
    keywords     NVARCHAR(500),
    active       BIT           NOT NULL DEFAULT 1,
    created_date DATETIME2              DEFAULT GETDATE(),
    updated_date DATETIME2              DEFAULT GETDATE(),
    CONSTRAINT fk_doc_product FOREIGN KEY (product_id)
        REFERENCES seafood_products (id) ON DELETE SET NULL -- SP bị xóa → document vẫn tồn tại, product_id = NULL
);

-- ============================================================
-- INDEXES — Performance
-- ============================================================

-- Products
CREATE INDEX idx_products_category ON seafood_products (category_id);
CREATE INDEX idx_products_active ON seafood_products (active);
CREATE INDEX idx_products_price ON seafood_products (price);
CREATE INDEX idx_products_expiry ON seafood_products (expiry_date) WHERE expiry_date IS NOT NULL AND active = 1;
CREATE INDEX idx_products_imported ON seafood_products (imported_date) WHERE imported_date IS NOT NULL;

-- Product Images
CREATE INDEX idx_images_product ON product_images (product_id);
CREATE UNIQUE INDEX uidx_images_primary ON product_images (product_id) WHERE is_primary = 1;

-- Combos
CREATE INDEX idx_combos_active ON combos (active);
CREATE INDEX idx_combos_valid ON combos (valid_until, active);

-- Combo Items
CREATE INDEX idx_comboitems_combo ON combo_items (combo_id);
CREATE INDEX idx_comboitems_product ON combo_items (product_id);

-- Cart indexes removed (carts & cart_items table dropped)

-- Orders
CREATE INDEX idx_orders_customer ON orders (customer_id);
CREATE INDEX idx_orders_status ON orders (order_status);
CREATE INDEX idx_orders_date ON orders (created_date DESC);

-- Order Items
CREATE INDEX idx_orderitems_order ON order_items (order_id);
CREATE INDEX idx_orderitems_combo ON order_items (combo_id);
CREATE INDEX idx_orderitems_product ON order_items (product_id) WHERE product_id IS NOT NULL;

-- Order Status History
CREATE INDEX idx_history_order ON order_status_history (order_id);
CREATE INDEX idx_history_time ON order_status_history (changed_at DESC);

-- Reviews
CREATE INDEX idx_reviews_product ON product_reviews (product_id);
CREATE INDEX idx_reviews_rating ON product_reviews (rating);

-- Wishlist
CREATE INDEX idx_wish_user ON wishlists (user_id);

-- Addresses
CREATE INDEX idx_addresses_user ON user_addresses (user_id);

-- Coupons
CREATE INDEX idx_coupon_code ON coupons (code);
CREATE INDEX idx_coupon_valid ON coupons (valid_until, active);

-- Coupon Usages
CREATE INDEX idx_usage_coupon ON coupon_usages (coupon_code);
CREATE INDEX idx_usage_user ON coupon_usages (user_id);

-- Knowledge
CREATE INDEX idx_knowledge_cat ON knowledge_documents (category);
CREATE INDEX idx_knowledge_active ON knowledge_documents (active);
CREATE INDEX idx_knowledge_product ON knowledge_documents (product_id);

-- ============================================================
-- FULL-TEXT SEARCH (tùy chọn — tăng hiệu quả RAG chatbot)
-- ============================================================
-- Uncomment để bật Full-Text Index trên knowledge_documents:
-- Bước 1: Bật Full-Text Search feature trong SQL Server
-- Bước 2: Chạy script sau
/*
CREATE FULLTEXT CATALOG ft_seafood_catalog AS DEFAULT;
CREATE FULLTEXT INDEX ON knowledge_documents(title, content, keywords)
    KEY INDEX PK__knowledge_documents__3213E83F  -- thay bằng tên PK thực tế
    ON ft_seafood_catalog
    WITH CHANGE_TRACKING AUTO;
*/
-- Sau khi bật, query RAG sẽ dùng:
--   SELECT * FROM knowledge_documents
--   WHERE CONTAINS((content, keywords, title), '"cá hồi" OR "salmon"')
--   AND active = 1  ORDER BY ...

-- ============================================================
-- SAMPLE DATA — Admin account
-- ============================================================
-- BCrypt hash của 'admin123' (strength=12)
INSERT INTO users (username, email, password, full_name, role, enabled)
VALUES ('admin',
        'admin@oceanic.com',
        '$2y$10$F0kSyqTY.PSKYS8XIqJpx.yAGhe7HFUqkwgMCxfyPixuz73DsEENu',
        'Oceanic Admin',
        'ADMIN',
        1);

-- ============================================================
-- SAMPLE CATEGORIES — Danh mục hải sản chuẩn
-- ============================================================
INSERT INTO categories (name, description)
VALUES (N'Cá tươi', N'Các loại cá tươi sống nhập mỗi ngày'),
       (N'Cá đông lạnh', N'Cá đã qua đông lạnh, bảo quản dài hạn'),
       (N'Hải sản tươi', N'Tôm, cua, mực, sò, nghêu tươi sống'),
       (N'Hải sản đông lạnh', N'Hải sản đông lạnh đảm bảo chất lượng'),
       (N'Chế biến sẵn', N'Sản phẩm đã qua sơ chế, chỉ cần nấu chín'),
       (N'Đặc sản', N'Hải sản cao cấp, đặc sản theo vùng miền');


-- ============================================================
-- SAMPLE COUPONS — Nghiệp vụ thực tế cho seafood store
-- ============================================================
--
-- Chiến lược coupon:
--   1. Chào mừng khách mới         → WELCOME10
--   2. Khuyến khích mua số lượng lớn → BULK15
--   3. Khách hàng thân thiết        → LOYAL20
--   4. Flash sale theo mùa          → SUMMER30
--   5. Đơn hàng nhỏ / thử nghiệm   → TRY5
--
INSERT INTO coupons (code, discount_type, discount_value, min_order_value, max_uses, valid_until, active)
VALUES
    -- Chào mừng khách hàng mới: giảm 10%, đơn tối thiểu $50, dùng không giới hạn 1 năm
    ('WELCOME10', 'PERCENT', 10.00, 50.00, 1000, DATEADD(year, 1, GETDATE()), 1),

    -- Mua số lượng lớn (wholesale/bulk): giảm 15%, đơn tối thiểu $150
    ('BULK15', 'PERCENT', 15.00, 150.00, 200, DATEADD(month, 6, GETDATE()), 1),

    -- Khách hàng thân thiết (gửi qua email): giảm $20 cố định, đơn tối thiểu $100
    ('LOYAL20', 'FIXED', 20.00, 100.00, 300, DATEADD(month, 6, GETDATE()), 1),

    -- Flash sale mùa hè: giảm 30%, dùng tối đa 50 lần, hết hạn 30 ngày
    ('SUMMER30', 'PERCENT', 30.00, 80.00, 50, DATEADD(day, 30, GETDATE()), 1),

    ('TRY5', 'FIXED', 5.00, 0.00, 500, DATEADD(month, 3, GETDATE()), 1);

-- ============================================================
-- SAMPLE COMBOS — Combo Hải sản
-- ============================================================
INSERT INTO combos (name, description, combo_price, stock_quantity, active)
VALUES (N'Combo Lẩu Hải Sản 4 người', N'Bao gồm tôm, mực, cá tươi chế biến sẵn cho lẩu', 499.00, 50, 1),
       (N'Combo BBQ Biển', N'Set hải sản nướng gồm bạch tuộc, hào, nghêu', 699.00, 30, 1),
       (N'Combo Ăn Thử Giảm Giá', N'Gồm các mặt hàng nhỏ để trải nghiệm', 199.00, 100, 1);

-- ⚠️ Uncomment sau khi đã INSERT seafood_products mẫu
-- INSERT INTO combo_items (combo_id, product_id, quantity)
-- VALUES
--     (1, 1, 2), -- VD: Combo 1 gồm 2 sản phẩm ID 1
--     (1, 3, 1), -- và 1 sản phẩm ID 3
--     (2, 2, 2), -- Combo 2 gồm 2 sản phẩm ID 2
--     (2, 4, 1),
--     (3, 1, 1),
--     (3, 5, 2);
-- ============================================================
-- KNOWLEDGE DOCUMENTS — Dữ liệu cho RAG Chatbot
-- ============================================================
-- Phân loại (category):
--   'POLICY'    → Chính sách cửa hàng
--   'FAQ'       → Câu hỏi thường gặp
--   'PRODUCT'   → Thông tin sản phẩm / hải sản chung
--   'GUIDE'     → Hướng dẫn bảo quản, chế biến

INSERT INTO knowledge_documents (title, content, keywords, category, active, created_date, updated_date)
VALUES

-- ============================================================
-- POLICY — Chính sách
-- ============================================================
(N'Chính sách giao hàng',
 N'Oceanic giao hàng toàn quốc. Miễn phí giao hàng cho đơn từ 500.000đ trở lên. '
     + N'Đơn dưới 500.000đ áp dụng phí ship 30.000đ (nội thành) hoặc 50.000đ (ngoại thành/tỉnh). '
     + N'Thời gian giao hàng: 2–4 giờ (nội thành TP.HCM & Hà Nội), 1–3 ngày làm việc (các tỉnh khác). '
     + N'Hải sản tươi sẽ được đóng gói thùng xốp + đá khô để đảm bảo nhiệt độ trong quá trình vận chuyển.',
 'POLICY',
 N'giao hàng, vận chuyển, phí ship, miễn phí ship, thời gian giao, đóng gói',
 1,
 GETDATE(),
 GETDATE()),

(
 N'Chính sách đổi trả và hoàn tiền',
 N'Oceanic cam kết 100% hoàn tiền hoặc đổi hàng nếu sản phẩm không đạt chất lượng khi nhận. '
     + N'Khách hàng cần chụp ảnh/video sản phẩm ngay khi nhận và liên hệ hotline trong vòng 2 giờ. '
     + N'Trường hợp được chấp nhận: sản phẩm không tươi, thiếu số lượng, sai hàng so với đơn đặt. '
     + N'Hoàn tiền qua chuyển khoản trong vòng 24–48 giờ làm việc sau khi được xác nhận. '
     + N'Không áp dụng đổi trả với lý do cá nhân (không thích, mua nhầm) sau khi đã giao hàng thành công.',
 'POLICY',
 N'đổi trả, hoàn tiền, khiếu nại, không tươi, sai hàng, chất lượng, refund',
 1,
 GETDATE(),
 GETDATE()),

(
 N'Chính sách thanh toán',
 N'Oceanic hỗ trợ 2 phương thức thanh toán: '
     + N'1. COD (Thanh toán khi nhận hàng): Khách trả tiền mặt cho shipper khi nhận hàng. '
     + N'2. Chuyển khoản ngân hàng (BANK_TRANSFER): Chuyển khoản trước, đính kèm ảnh biên lai lên hệ thống. '
     + N'   Admin xác nhận thanh toán trong vòng 15–30 phút (giờ hành chính). '
     + N'Số tài khoản: Vietcombank – 1234567890 – Công ty TNHH Oceanic Seafood. '
     + N'Nội dung chuyển khoản ghi: Mã đơn hàng (ví dụ: DH00123).',
 'POLICY',
 N'thanh toán, COD, chuyển khoản, ngân hàng, tiền mặt, tài khoản, Vietcombank',
 1,
 GETDATE(),
 GETDATE()),

(
 N'Chính sách bảo mật thông tin khách hàng',
 N'Oceanic cam kết bảo mật toàn bộ thông tin cá nhân của khách hàng. '
     + N'Dữ liệu (họ tên, số điện thoại, địa chỉ) chỉ được dùng để xử lý đơn hàng và liên hệ chăm sóc khách hàng. '
     + N'Oceanic không bán hoặc chia sẻ thông tin với bên thứ ba dưới bất kỳ hình thức nào. '
     + N'Khách hàng có thể yêu cầu xóa tài khoản và toàn bộ dữ liệu cá nhân bất cứ lúc nào.',
 'POLICY',
 N'bảo mật, thông tin cá nhân, quyền riêng tư, dữ liệu, tài khoản',
 1,
 GETDATE(),
 GETDATE()),

-- ============================================================
-- FAQ — Câu hỏi thường gặp
-- ============================================================
(
 N'Làm sao để đặt hàng?',
 N'Đặt hàng tại Oceanic rất đơn giản: '
     + N'Bước 1: Đăng ký tài khoản hoặc đăng nhập. '
     + N'Bước 2: Chọn sản phẩm, thêm vào giỏ hàng. '
     + N'Bước 3: Vào giỏ hàng, kiểm tra đơn hàng, nhập mã giảm giá (nếu có). '
     + N'Bước 4: Chọn địa chỉ giao hàng và phương thức thanh toán. '
     + N'Bước 5: Xác nhận đặt hàng. Bạn sẽ nhận email/SMS xác nhận ngay sau đó.',
 'FAQ',
 N'đặt hàng, mua hàng, giỏ hàng, hướng dẫn mua, checkout',
 1,
 GETDATE(),
 GETDATE()),

(
 N'Mã giảm giá (coupon) sử dụng như thế nào?',
 N'Mã giảm giá được nhập tại trang thanh toán (checkout) trong ô "Mã giảm giá". '
     + N'Các mã hiện có: '
     + N'- WELCOME10: Giảm 10% cho khách mới, đơn tối thiểu 50.000đ. '
     + N'- BULK15: Giảm 15% cho đơn từ 150.000đ trở lên (mua số lượng lớn). '
     + N'- LOYAL20: Giảm 20.000đ cố định cho đơn từ 100.000đ (khách thân thiết). '
     + N'- SUMMER30: Giảm 30% cho đơn từ 80.000đ (flash sale mùa hè, giới hạn 50 lượt). '
     + N'- TRY5: Giảm 5.000đ, không yêu cầu đơn tối thiểu (dùng thử). '
     + N'Lưu ý: Mỗi tài khoản chỉ dùng mỗi mã 1 lần duy nhất.',
 'FAQ',
 N'mã giảm giá, coupon, khuyến mãi, discount, WELCOME10, BULK15, LOYAL20, SUMMER30, TRY5',
 1,
 GETDATE(),
 GETDATE()),

(
 N'Tôi có thể theo dõi đơn hàng ở đâu?',
 N'Khách hàng có thể theo dõi trạng thái đơn hàng tại mục "Đơn hàng của tôi" sau khi đăng nhập. '
     + N'Các trạng thái đơn hàng: '
     + N'- PENDING (Chờ xác nhận): Đơn vừa đặt, đang chờ admin xử lý. '
     + N'- CONFIRMED (Đã xác nhận): Admin đã xác nhận đơn hàng. '
     + N'- PROCESSING (Đang chuẩn bị): Hàng đang được đóng gói và chuẩn bị giao. '
     + N'- SHIPPED (Đang giao): Shipper đang trên đường giao hàng đến bạn. '
     + N'- DELIVERED (Đã giao): Đơn hàng giao thành công. '
     + N'- CANCELLED (Đã hủy): Đơn đã bị hủy.',
 'FAQ',
 N'theo dõi đơn hàng, trạng thái, PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED, đơn của tôi',
 1,
 GETDATE(),
 GETDATE()),

(
 N'Làm sao hủy đơn hàng?',
 N'Khách hàng có thể tự hủy đơn hàng khi đơn còn ở trạng thái PENDING (Chờ xác nhận). '
     + N'Cách hủy: Vào "Đơn hàng của tôi" → Chọn đơn cần hủy → Nhấn "Hủy đơn". '
     + N'Khi đơn đã chuyển sang CONFIRMED hoặc các trạng thái sau, không thể tự hủy qua website. '
     + N'Vui lòng liên hệ hotline để được hỗ trợ hủy thủ công trong trường hợp khẩn cấp.',
 'FAQ',
 N'hủy đơn, cancel order, đổi ý, không muốn mua nữa',
 1,
 GETDATE(),
 GETDATE()),

(
 N'Giờ làm việc và thông tin liên hệ',
 N'Oceanic Seafood hoạt động từ 6:00 sáng đến 10:00 tối, tất cả các ngày trong tuần kể cả lễ tết. '
     + N'Hotline: 1800-6868 (miễn phí, 6:00–22:00). '
     + N'Email hỗ trợ: support@oceanic.com. '
     + N'Địa chỉ cửa hàng: 123 Nguyễn Thị Minh Khai, Quận 1, TP. Hồ Chí Minh. '
     + N'Fanpage Facebook: facebook.com/OceanicSeafood. '
     + N'Zalo OA: Oceanic Seafood.',
 'FAQ',
 N'liên hệ, hotline, giờ làm việc, địa chỉ, email, zalo, facebook, hỗ trợ',
 1,
 GETDATE(),
 GETDATE()),

(
 N'Combo hải sản là gì? Có những combo nào?',
 N'Combo là gói nhiều sản phẩm hải sản được đóng sẵn theo chủ đề với mức giá ưu đãi hơn mua lẻ. '
     + N'Các combo hiện có tại Oceanic: '
     + N'1. Combo Lẩu Hải Sản 4 người (499.000đ): Bao gồm tôm, mực, cá tươi chế biến sẵn cho nồi lẩu. '
     + N'2. Combo BBQ Biển (699.000đ): Set hải sản nướng gồm bạch tuộc, hào, nghêu — phù hợp tiệc nướng ngoài trời. '
     + N'3. Combo Ăn Thử Giảm Giá (199.000đ): Gồm các mặt hàng nhỏ để trải nghiệm nhiều loại hải sản. '
     + N'Combo có số lượng có hạn — đặt sớm để không hết hàng!',
 'FAQ',
 N'combo, set hải sản, lẩu, BBQ, gói ưu đãi, combo lẩu, combo nướng',
 1,
 GETDATE(),
 GETDATE());
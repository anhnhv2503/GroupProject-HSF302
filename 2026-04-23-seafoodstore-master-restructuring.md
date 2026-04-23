# SeafoodStore Comprehensive Master Restructuring Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Chuyển đổi toàn diện dự án sang kiến trúc chuyên nghiệp: Backend tuân thủ Layered Architecture (Service Layer), Frontend tuân thủ Atomic Design & Decorator Layout.

**Architecture:** 
- **Backend:** Triệt tiêu hoàn toàn việc Controller gọi Repository trực tiếp. Mọi logic nghiệp vụ phải đi qua Service.
- **Frontend:** Hệ thống Layout trung tâm (`main.html`) điều khiển toàn bộ giao diện. Chia nhỏ UI thành các thành phần tái sử dụng (Components) và các khối khung (Layout Fragments).

**Tech Stack:** Spring Boot 3.x, Thymeleaf, Tailwind CSS (Design Tokens), Service Layer Pattern.

---

### Task 1: Chuẩn hóa Design Tokens & Cấu trúc Thư mục

**Files:**
- Create: `src/main/resources/templates/layout/main.html`
- Create: `src/main/resources/templates/fragments/layout/`
- Create: `src/main/resources/templates/fragments/components/`

**Step 1: Tạo thư mục cấu trúc Atomic**
Run: `mkdir -p src/main/resources/templates/layout src/main/resources/templates/fragments/layout src/main/resources/templates/fragments/components`

**Step 2: Triển khai Head Fragment (Centralized Tokens)**
Xây dựng `fragments/layout/head.html` chứa mã màu `#D35400`, font `Playfair Display` và cấu hình Tailwind. Đây là nguồn dữ liệu UI duy nhất.

**Step 3: Commit cấu trúc nền tảng**
```bash
git add src/main/resources/templates/
git commit -m "chore: initialize atomic design directory structure and central tokens"
```

---

### Task 2: Xây dựng Hệ thống Khung (Core Shell)

**Files:**
- Create: `src/main/resources/templates/layout/main.html`
- Create: `src/main/resources/templates/fragments/layout/header.html`
- Create: `src/main/resources/templates/fragments/layout/footer.html`

**Step 1: Code Header & Footer chuẩn UI/UX Pro Max**
Sử dụng Glassmorphism cho Header và thiết kế Footer 4 cột chuyên nghiệp. Đảm bảo mọi icon đều dùng Material Symbols.

**Step 2: Code Base Layout (`main.html`)**
Thiết lập file khung xương để các trang con kế thừa. Phải bao gồm các khối: `head`, `header`, `content`, `footer`, `scripts`.

**Step 3: Commit Core Shell**
```bash
git add src/main/resources/templates/layout/ src/main/resources/templates/fragments/layout/
git commit -m "feat: implement master layout shell and premium fragments"
```

---

### Task 3: Refactor Toàn bộ Trang Chủ (Client Side)

**Files:**
- Modify: `src/main/resources/templates/index.html`
- Modify: `src/main/resources/templates/fragments/components/product-card.html`

**Step 1: Chuẩn hóa Product Card**
Cập nhật `product-card.html` để sử dụng các token màu đã định nghĩa ở Task 1. Thêm `aria-label` cho các nút.

**Step 2: Áp dụng Layout vào index.html**
Xóa bỏ code HTML thừa trong `index.html`. Chỉ giữ lại các Section nội dung và bọc chúng trong `th:replace="~{layout/main :: layout(...)}"`.

**Step 3: Commit Client Refactor**
```bash
git add src/main/resources/templates/index.html
git commit -m "refactor: migrate homepage to standardized layout system"
```

---

### Task 4: Chuẩn hóa Backend (Controller Audit)

**Files:**
- Modify: `src/main/java/com/project/hsf/controller/CheckoutController.java`
- Audit: Tất cả các Controller trong `controller/client/` và `controller/admin/`

**Step 1: Kiểm tra việc sử dụng Repository**
Tìm kiếm tất cả các Controller đang `@Autowired` Repository.
Run: `grep -r "Repository" src/main/java/com/project/hsf/controller/`

**Step 2: Chuyển sang Service Layer**
Thay thế Repository bằng Service tương ứng. Đảm bảo Controller chỉ điều hướng và gọi Service để lấy dữ liệu.

**Step 3: Commit Backend Standardization**
```bash
git commit -am "refactor: enforce layered architecture across all controllers"
```

---

### Task 4: Mở rộng Layout cho hệ thống Admin

**Files:**
- Create: `src/main/resources/templates/layout/admin-layout.html`
- Modify: `src/main/resources/templates/admin/product-manage.html`

**Step 1: Tạo Layout riêng cho Admin**
Nếu Admin có giao diện khác (Sidebar), tạo một file layout riêng nhưng vẫn dùng chung Design Tokens ở Task 1.

**Step 2: Áp dụng cho các trang quản trị**
Chuyển đổi `product-manage.html` và `order-manage.html` sang dùng layout mới.

**Step 3: Commit Admin Refactor**
```bash
git commit -am "feat: extend layout system to admin dashboard"
```
yêu cầu: 
chỉ tạo lại cấu trúc rồi thôi á
tạo package
rồi quăng từng cái vào đúng chỗ thôi
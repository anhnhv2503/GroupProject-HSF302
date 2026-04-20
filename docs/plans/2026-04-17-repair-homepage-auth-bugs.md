# Repair Homepage and Auth Bugs Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Fix the registration form mismatch preventing new users from signing up, and restore the missing 'New Arrivals' section on the homepage.

**Architecture:**
- Update `register.html` to align its structure with `RegisterDto`, including the missing `email` and `confirmPassword` inputs, and proper display of validation errors.
- Update `index.html` to render the `newArrivals` data list passed in by `HomeController`, matching the exact styling of the Best Sellers section.

**Tech Stack:** Spring Boot 3, Thymeleaf, Spring Validation, HTML/CSS.

---

### Task 1: Fix Registration Form Structure and Fields

**Files:**
- Modify: `src/main/resources/templates/auth/register.html`

**Step 1: Write the minimal implementation**
We need to sync the form inputs in `register.html` with `RegisterDto.java` and expose validation errors. 
- The form currently lacks `email` and `confirmPassword`, which causes `isPasswordMatch()` to always fail.
- The form contains an unused `address` field.
- The form does not display global or field validation errors correctly.

Change the `<form method="post" action="/register">` completely to properly use `th:object="${form}"` and include the correct fields.

```html
<!-- Inside src/main/resources/templates/auth/register.html -->
<!-- Replace the <form> content with the updated structure: -->
        <form method="post" th:action="@{/register}" th:object="${form}">
          
          <!-- Global error messages including password mismatch -->
          <div th:if="${errorMsg}" class="alert alert-danger py-2 small mb-4 border-0" style="background: rgba(186,26,26,0.1); color: var(--error);" th:text="${errorMsg}"></div>
          
          <div class="row g-3">
            <div class="col-md-12 auth-input-group">
              <label>Họ và tên</label>
              <input type="text" th:field="*{fullName}" placeholder="Nguyễn Văn A" required/>
              <div th:if="${#fields.hasErrors('fullName')}" th:errors="*{fullName}" class="text-danger small mt-1"></div>
            </div>
            
            <div class="col-md-12 auth-input-group">
              <label>Tên đăng nhập</label>
              <input type="text" th:field="*{username}" placeholder="Chương trình viên" required/>
              <div th:if="${#fields.hasErrors('username')}" th:errors="*{username}" class="text-danger small mt-1"></div>
            </div>

            <div class="col-md-12 auth-input-group">
              <label>Email</label>
              <input type="email" th:field="*{email}" placeholder="example@email.com" required/>
              <div th:if="${#fields.hasErrors('email')}" th:errors="*{email}" class="text-danger small mt-1"></div>
            </div>

            <div class="col-md-12 auth-input-group">
              <label>Số điện thoại</label>
              <input type="tel" th:field="*{phone}" placeholder="0901234567" required/>
              <div th:if="${#fields.hasErrors('phone')}" th:errors="*{phone}" class="text-danger small mt-1"></div>
            </div>
            
            <div class="col-md-6 auth-input-group">
              <label>Mật khẩu</label>
              <input type="password" th:field="*{password}" placeholder="••••••••" required/>
              <div th:if="${#fields.hasErrors('password')}" th:errors="*{password}" class="text-danger small mt-1"></div>
            </div>
            
            <div class="col-md-6 auth-input-group">
              <label>Xác nhận mật khẩu</label>
              <input type="password" th:field="*{confirmPassword}" placeholder="••••••••" required/>
              <div th:if="${#fields.hasErrors('confirmPassword')}" th:errors="*{confirmPassword}" class="text-danger small mt-1"></div>
            </div>
          </div>

          <button type="submit" class="btn btn-primary w-100 py-3 mt-4 fw-900" 
                  style="background:linear-gradient(135deg, var(--secondary), var(--tertiary)); border:none; border-radius:12px; box-shadow: 0 8px 20px rgba(46,97,146,0.2);">
            TẠO TÀI KHOẢN <i class="bi bi-person-plus ms-2"></i>
          </button>
        </form>
```

**Step 2: Commit**
```bash
git add src/main/resources/templates/auth/register.html
git commit -m "fix: align register form fields with RegisterDto to fix registration bug"
```

---

### Task 2: Restore "New Arrivals" Section on Homepage

**Files:**
- Modify: `src/main/resources/templates/index.html`

**Step 1: Write minimal implementation**
The `HomeController` injects a `newArrivals` list into the template model. We must add the UI section to display this immediately after the `bestSellers` list, using the same styling. 

```html
<!-- Inside src/main/resources/templates/index.html -->
<!-- Insert this immediately after the existing Best Sellers section (before Features) -->

  <!-- ── New Arrivals ── -->
  <div class="container my-5 py-5" style="max-width:1440px; padding-inline:2rem;">
    <div class="d-flex justify-content-between align-items-end mb-5">
      <h2 class="section-heading-premium mb-0 text-uppercase" style="font-size:1.8rem; letter-spacing:0.05em;">SẢN PHẨM <span style="font-weight:300; color:var(--on-surface);">MỚI NHẤT</span></h2>
      <a href="/products" class="fw-bold text-dark text-uppercase" style="text-decoration:underline; font-size:0.85rem; letter-spacing:0.05em;">TẤT CẢ SẢN PHẨM <i class="bi bi-arrow-right"></i></a>
    </div>

    <div class="row g-4" th:if="${!#lists.isEmpty(newArrivals)}">
      <div th:each="p : ${newArrivals}" class="col-sm-6 col-md-4 col-xl-3">
        <div class="product-card-premium">
          <div class="product-img-wrap">
            <div class="catch-badge-wrap">
              <span th:if="${p.freshnessStatus == 'FRESH'}" class="catch-badge badge-fresh">Tươi rói</span>
              <span th:if="${p.freshnessStatus == 'FROZEN'}" class="catch-badge badge-frozen">Cấp đông</span>
              <span class="catch-badge badge-hot">Mới về</span>
            </div>
            <img th:src="${p.imageUrl != null ? p.imageUrl : 'https://images.unsplash.com/photo-1534482421-64566f976cfa?w=600&q=80'}" 
                 th:alt="${p.name}"/>
          </div>
          <div class="p-4 d-flex flex-column flex-grow-1">
            <h3 class="h6 fw-800 mb-1" style="min-height: 2.6rem; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;" th:text="${p.name}">Sản phẩm</h3>
            <p class="small text-muted mb-4" style="font-size: 0.8rem; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;" th:text="${p.description}">Mô tả ngắn</p>
            
            <div class="mt-auto">
              <div class="d-flex justify-content-between align-items-center mb-3">
                <span class="h5 fw-900 text-primary mb-0 font-roboto" th:text="${#numbers.formatDecimal(p.price,0,'COMMA',0,'POINT') + ' ₫'}">0 ₫</span>
                <span class="small fw-700 text-secondary-emphasis bg-light px-2 py-1 rounded font-roboto" style="font-size: 0.7rem;" th:text="${p.soldCount + ' đã bán'}">0 đã bán</span>
              </div>
              
              <form method="post" action="/cart/add" sec:authorize="isAuthenticated()">
                <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
                <input type="hidden" name="productId" th:value="${p.id}"/>
                <input type="hidden" name="quantity" value="1"/>
                <button type="submit" class="btn w-100 py-2" 
                        style="background:var(--surface-container-highest); color:var(--on-surface); border:none; border-radius:2px; font-weight:700; text-transform:uppercase; font-size:0.85rem; letter-spacing:0.05em;">
                  THÊM VÀO GIỎ
                </button>
              </form>
              <a href="/login" class="btn w-100 py-2 text-center" style="background:var(--surface-container-highest); color:var(--on-surface); border:none; border-radius:2px; font-weight:700; text-transform:uppercase; font-size:0.85rem; letter-spacing:0.05em; display:block; text-decoration:none;" sec:authorize="isAnonymous()">
                Đăng nhập
              </a>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <div class="text-center py-5 border rounded-4 border-dashed" th:if="${#lists.isEmpty(newArrivals)}" style="border: 2px dashed var(--outline-variant) !important; background: rgba(0,0,0,0.02);">
      <i class="bi bi-inbox fs-1 text-muted opacity-25 d-block mb-3"></i>
      <h5 class="fw-700 text-muted">Chưa có sản phẩm mới</h5>
      <p class="text-muted small">Kho hải sản đang chờ đón các mẻ cập bến mới nhất.</p>
    </div>
  </div>
```

**Step 2: Commit**
```bash
git add src/main/resources/templates/index.html
git commit -m "fix: render missing newArrivals section on the homepage"
```

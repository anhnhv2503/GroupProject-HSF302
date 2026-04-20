# Hoàn thiện Luồng Login/Logout & UI Trang Chủ Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Hoàn thiện toàn bộ luồng đăng nhập, đăng ký và đăng xuất, khắc phục lỗi redirect 302 và cập nhật UI trang chủ (`index.html`) để hiển thị các tùy chọn phù hợp.

**Architecture:** 
- Sửa lỗi thiếu thuộc tính `name` trong form đăng nhập.
- Cấu hình `SecurityConfig` để hỗ trợ hiển thị thông báo lỗi và thông báo đăng xuất.
- Cập nhật Header của `index.html` sử dụng `sec:authorize` để hiển thị nút Đăng nhập/Đăng ký hoặc Thông tin người dùng.
- Tích hợp form đăng xuất chuẩn POST trong trang chủ.


---

### Task 1: Cấu hình Bảo mật & Controller

**Files:**
- Modify: `src/main/java/com/project/hsf/config/SecurityConfig.java`
- Modify: `src/main/java/com/project/hsf/controller/AuthController.java`

**Step 1: Public URLs**
Thêm `/products/**` và `/webjars/**` vào `publicUrl` list.

**Step 2: Commit**
```bash
git add src/main/java/com/project/hsf/config/SecurityConfig.java
git commit -m "security: update public URLs and enhance login/logout flow"
```

### Task 2: Tích hợp Auth UI vào index.html

**Files:**
- Modify: `src/main/resources/templates/index.html`

**Step 1: Thêm Security Namespace**
```html
<html class="light" lang="vi" xmlns:th="http://www.thymeleaf.org" xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
```

**Step 2: Cập nhật Header Auth Section**
Tìm đến đoạn thẻ `<div class="flex items-center gap-4 text-slate-900">` và thay thế logic icon bằng:
```html
            <!-- Auth Section -->
            <div class="flex items-center gap-3">
                <!-- Not Authenticated -->
                <div sec:authorize="!isAuthenticated()" class="flex items-center gap-2">
                    <a th:href="@{/login}" class="text-xs font-bold uppercase tracking-widest text-on-surface-variant hover:text-primary transition-colors">Đăng nhập</a>
                    <span class="text-outline-variant/30">|</span>
                    <a th:href="@{/register}" class="bg-primary-container text-white px-4 py-2 rounded-full text-[10px] font-black uppercase tracking-widest hover:opacity-90 shadow-md shadow-primary-container/20 transition-all">Đăng ký</a>
                </div>

                <!-- Authenticated -->
                <div sec:authorize="isAuthenticated()" class="flex items-center gap-4">
                    <div class="flex items-center gap-2 group cursor-pointer relative py-2">
                        <span class="material-symbols-outlined text-secondary" data-icon="account_circle">account_circle</span>
                        <div class="flex flex-col">
                            <span class="text-[10px] font-black uppercase tracking-tighter text-on-surface" sec:authentication="principal.username">Username</span>
                            <span class="text-[8px] text-on-surface-variant leading-none">Thành viên quý tộc</span>
                        </div>
                        
                        <!-- Mini Dropdown -->
                        <div class="absolute top-full right-0 mt-1 w-48 bg-white rounded-xl shadow-xl border border-outline-variant/10 opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all z-50 p-2">
                            <a href="/profile" class="flex items-center gap-2 p-2 hover:bg-surface-container-low rounded-lg text-xs font-medium transition-colors">
                                <span class="material-symbols-outlined text-sm">person</span> Hồ sơ
                            </a>
                            <hr class="my-1 border-outline-variant/10">
                            <form th:action="@{/logout}" method="post">
                                <button type="submit" class="w-full flex items-center gap-2 p-2 hover:bg-error-container/10 text-error rounded-lg text-xs font-bold transition-colors">
                                    <span class="material-symbols-outlined text-sm">logout</span> Đăng xuất
                                </button>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
```


Login thành công thì có lời chào Xin chào, [username] của người dùng

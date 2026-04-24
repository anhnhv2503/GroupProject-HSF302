# 🌊 The Culinary Curator (SeafoodStoreAI)

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-brightgreen)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**The Culinary Curator** là một nền tảng thương mại điện tử hiện đại chuyên cung cấp hải sản cao cấp, tích hợp trí tuệ nhân tạo (AI) để hỗ trợ khách hàng và tối ưu hóa trải nghiệm mua sắm. Dự án được xây dựng với kiến trúc Spring Boot mạnh mẽ và hệ thống thiết kế "Premium Editorial".

---

## Tính năng nổi bật (Key Features)

### Trải nghiệm người dùng (Customer Experience)
- **Hệ thống cửa hàng hiện đại**: Giao diện responsive, tối ưu hóa cho cả máy tính và thiết bị di động.
- **Giỏ hàng & Thanh toán**: Hỗ trợ giỏ hàng linh hoạt, tích hợp cổng thanh toán **PayOS**.
- **Quản lý đơn hàng**: Theo dõi trạng thái đơn hàng thời gian thực với lịch sử thay đổi chi tiết.
- **Yêu thích & Đánh giá**: Cho phép người dùng lưu sản phẩm yêu thích và để lại đánh giá sau khi mua hàng.
- **Hệ thống Mã giảm giá**: Áp dụng coupon linh hoạt theo chương trình khuyến mãi.

### Tích hợp AI (AI Integration)
- **Chatbot AI**: Sử dụng **LangChain4j** kết hợp với **Google Gemini API** để tư vấn sản phẩm, giải đáp thắc mắc của khách hàng dựa trên cơ sở kiến thức (Knowledge Base) của cửa hàng.
- **Semantic Search**: Tìm kiếm sản phẩm thông minh dựa trên ý nghĩa thay vì chỉ từ khóa.

### Quản trị hệ thống (Admin Dashboard)
- **Quản lý Sản phẩm**: CRUD sản phẩm hải sản, danh mục, combo sản phẩm và hình ảnh.
- **Quản lý Banner**: Tùy chỉnh giao diện trang chủ dễ dàng qua trình quản lý banner.
- **Quản lý Người dùng & Đơn hàng**: Kiểm soát toàn bộ luồng vận hành từ khi khách đặt hàng đến khi giao hàng thành công.
- **Hệ thống Coupon**: Tạo và quản lý các chiến dịch giảm giá.

---

## Kiến trúc kỹ thuật (Technical Architecture)

### Backend
- **Framework**: Spring Boot 4.0.5 (Java 21)
- **Security**: Spring Security 6 (RBAC - Role Based Access Control)
- **Data Access**: Spring Data JPA
- **Database**: Microsoft SQL Server
- **AI Engine**: LangChain4j, Google Gemini AI
- **Integrations**: PayOS (Payment Gateway), Cloudinary (Image Hosting)

### Frontend
- **Template Engine**: Thymeleaf (với Layout Dialect)
- **CSS Framework**: Tailwind CSS  + Vanilla CSS (Custom Design System)
- **Design System**: Stitch Premium Editorial (Editorial Typography, Ghost Borders, Dynamic Animations)

---

## 📂 Cấu trúc thư mục (Project Structure)

```text
src/main/java/com/project/hsf/
├── config/             # Cấu hình Security, Cloudinary, MVC, AI...
├── controller/         # Xử lý Request (Admin & Client)
├── dto/                # Đối tượng vận chuyển dữ liệu
├── entity/             # Các thực thể JPA (Database Models)
├── enums/              # Các kiểu liệt kê (Status, Roles...)
├── repository/         # Tương tác với cơ sở dữ liệu
├── service/            # Business Logic
├── specification/      # Query lọc nâng cao (JPA Specification)
└── util/               # Các hàm tiện ích (Format, Mapping...)
```

---

## 🚀 Hướng dẫn cài đặt (Setup Instructions)

### 1. Yêu cầu hệ thống
- JDK 21+
- Maven 3.8+
- SQL Server

### 2. Cấu hình Database
Tạo cơ sở dữ liệu trên SQL Server và cấu hình trong file `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=SeafoodStore;encrypt=true;trustServerCertificate=true
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### 3. Cấu hình API Keys
Dự án yêu cầu một số API Key để hoạt động đầy đủ các tính năng:
- **Cloudinary**: Lưu trữ hình ảnh.
- **PayOS**: Thanh toán trực tuyến.
- **Google Gemini**: AI Chatbot.

### 4. Chạy ứng dụng
Sử dụng Maven để chạy project:
```bash
mvn spring-boot:run
```

---

## 🎨 Design System
Dự án tuân thủ bộ quy tắc thiết kế **Stitch Premium Editorial**:
- **Primary Color**: `#a63b00` (Main brand color)
- **Typography**: Inter (Editorial style)
- **Motion**: 16px/28px Border-radius, Cubic-bezier animations.

---

## 👥 Nhóm phát triển
Dự án được thực hiện bởi nhóm phát triển tại **HSF302**.

---
*Developed with HSF302 group 6*

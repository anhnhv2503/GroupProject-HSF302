# SeafoodStore Core Features Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement Spring Security authentication, Session cart + Checkout flow with Database Transactions, Gemini AI Chatbot, and CRUD for Knowledge Documents according to Codex conventions.

**Architecture:** Monolithic Spring Boot MVC. Session-based state mapping for the cart. Transacted checkout utilizing atomic UPDATE queries. Chatbot via explicit RestTemplate HTTP calls to Gemini 2.0 Flash APIs (RAG architecture with text substitution).

**Tech Stack:** Java 21, Spring Boot, Spring Security (BCrypt), Spring Data JPA, Thymeleaf, SQL Server, RestTemplate.

---

### Task 1: Auth & Security Foundation

**Files:**
- Create: `src/main/java/com/project/hsf/config/AppConfig.java`
- Create: `src/main/java/com/project/hsf/config/SecurityConfig.java`
- *(Note: UserRepository, UserService, UserDetailsServiceImpl, RegisterDTO, AuthController were created in previous steps).*

**Step 1: Implement AppConfig**
```java
package com.project.hsf.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

**Step 2: Implement SecurityConfig**
Create SecurityFilterChain, configure `/admin/**` requiring `ADMIN` role, setup form login with CSRF enabled, and configure user details service.

**Step 3: Compile to verify**
Run: `mvn clean compile`
Expected: BUILD SUCCESS (Ensures no syntax errors and checks config initialization).

**Step 4: Commit**
```bash
git add src/main/java/com/project/hsf/config/
git commit -m "feat: setup spring security and application beans"
```

---

### Task 2: Session Cart Implementation

**Files:**
- Create: `src/main/java/com/project/hsf/dto/CartItemDTO.java`
- Create: `src/main/java/com/project/hsf/service/CartService.java`
- Create: `src/main/java/com/project/hsf/controller/CartController.java`

**Step 1: Implement CartItemDTO**
```java
package com.project.hsf.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {
    private String itemKey; // "P_" + productId or "C_" + comboId
    private Integer productId;
    private Integer comboId;
    private String name;
    private Integer quantity;
    private Double unitPrice;
    
    public Double getSubtotal() {
        return this.quantity * this.unitPrice;
    }
}
```

**Step 2: Implement CartService**
Manipulate `HttpSession` directly using key `"CART"` (`Map<String, CartItemDTO>`). Add methods for add, update, remove, and calculate subtotal/total.

**Step 3: Implement CartController**
Expose endpoints for Thymeleaf views (`/cart`) and handle POST modify requests (checking CSRF).

**Step 4: Compile to verify**
Run: `mvn clean compile`
Expected: BUILD SUCCESS

**Step 5: Commit**
```bash
git add src/main/java/com/project/hsf/dto/CartItemDTO.java src/main/java/com/project/hsf/service/CartService.java src/main/java/com/project/hsf/controller/CartController.java
git commit -m "feat: implement session based cart operations"
```

---

### Task 3: Transactional Checkout Flow

**Files:**
- Create: `src/main/java/com/project/hsf/repository/SeafoodProductRepository.java`
- Create: `src/main/java/com/project/hsf/repository/CouponRepository.java`
- Create: `src/main/java/com/project/hsf/service/OrderService.java`
- Create: `src/main/java/com/project/hsf/controller/CheckoutController.java`

**Step 1: Implement Data Repositories with Atomic Updates**
Add `@Modifying(clearAutomatically = true)` and `@Query` to `SeafoodProductRepository`:
`UPDATE SeafoodProduct s SET s.stockQuantity = s.stockQuantity - :qty WHERE s.id = :id AND s.stockQuantity >= :qty AND s.active = true`
Repeat similar atomic update for `CouponRepository`.

**Step 2: Implement OrderService**
Create `placeOrder` method with `@Transactional(rollbackFor = Exception.class)`.
Flow: Atomic Deduct stock -> Construct Order -> Insert items -> Atomic Update Coupon -> Insert Payment. Throw custom exception immediately if affected rows is 0. 

**Step 3: Implement CheckoutController**
Handle GET `/checkout` and POST `/checkout/place-order`. Load session cart state and trigger `OrderService`.

**Step 4: Compile to verify**
Run: `mvn clean compile`
Expected: BUILD SUCCESS

**Step 5: Commit**
```bash
git add src/main/java/com/project/hsf/repository/ src/main/java/com/project/hsf/service/OrderService.java src/main/java/com/project/hsf/controller/CheckoutController.java
git commit -m "feat: implement robust transactional checkout flow"
```

---

### Task 4: Gemini Chatbot Backend

**Files:**
- Create: `src/main/java/com/project/hsf/repository/KnowledgeDocumentRepository.java`
- Create: `src/main/java/com/project/hsf/service/ChatbotService.java`
- Create: `src/main/java/com/project/hsf/controller/ChatbotController.java`

**Step 1: Implement KnowledgeDocumentRepository**
Add query `findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase` or similar.

**Step 2: Implement ChatbotService**
Fetch 3 documents max. Construct System Prompt mapping "Trợ lý hải sản Culinary Curator". Format JSON strictly for `generativelanguage.googleapis.com` endpoint. Inject API Key via `@Value("${gemini.api.key}")`. Transmit request via `RestTemplate`.

**Step 3: Implement ChatbotController**
Expose POST `/api/chat` to process prompt strings returning generated AI output.

**Step 4: Compile to verify**
Run: `mvn clean compile`
Expected: BUILD SUCCESS

**Step 5: Commit**
```bash
git add src/main/java/com/project/hsf/repository/KnowledgeDocumentRepository.java src/main/java/com/project/hsf/service/ChatbotService.java src/main/java/com/project/hsf/controller/ChatbotController.java
git commit -m "feat: integrate Gemini AI RAG chatbot backend"
```

---

### Task 5: Admin CRUD for Knowledge Documents

**Files:**
- Create: `src/main/java/com/project/hsf/controller/admin/AdminKnowledgeController.java`

**Step 1: Implement AdminKnowledgeController**
Inject `KnowledgeDocumentRepository`. Endpoints secured with `@PreAuthorize("hasRole('ADMIN')")`. Mappings for GET `/admin/knowledge`, POST `/admin/knowledge/create`, POST `/admin/knowledge/update`, POST `/admin/knowledge/delete` with proper Thymeleaf redirects.

**Step 2: Compile to verify**
Run: `mvn clean compile`
Expected: BUILD SUCCESS

**Step 3: Commit**
```bash
git add src/main/java/com/project/hsf/controller/admin/AdminKnowledgeController.java
git commit -m "feat: add admin knowledge document management"
```

# Setup Foundation & Entities Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Establish the Spring MVC package structure and implement core entities (User, KnowledgeDocument) mapped to the existing SQL Server schema.

**Architecture:** Standard 3-tier architecture. Entities will use Jakarta Persistence (JPA) annotations to map to the `users` and `knowledge_documents` tables.

**Tech Stack:** Java 21, Spring Boot 4.0.5, Spring Data JPA, Hibernate, Lombok.

---

### Task 1: Package Structure Initialization

**Files:**
- Create: `src/main/java/com/project/hsf/model/.keep`
- Create: `src/main/java/com/project/hsf/repository/.keep`
- Create: `src/main/java/com/project/hsf/service/.keep`
- Create: `src/main/java/com/project/hsf/service/impl/.keep`
- Create: `src/main/java/com/project/hsf/config/.keep`
- Create: `src/main/java/com/project/hsf/controller/.keep`
- Create: `src/main/java/com/project/hsf/dto/.keep`

**Step 1: Create directories**

Run: `mkdir -p src/main/java/com/project/hsf/model src/main/java/com/project/hsf/repository src/main/java/com/project/hsf/service/impl src/main/java/com/project/hsf/config src/main/java/com/project/hsf/controller src/main/java/com/project/hsf/dto`

**Step 2: Commit initialization**

```bash
git add src/main/java/com/project/hsf
git commit -m "chore: initialize project package structure"
```

---

### Task 2: Implement User Entity

**Files:**
- Create: `src/main/java/com/project/hsf/model/User.java`
- Create: `src/main/java/com/project/hsf/repository/UserRepository.java`

**Step 1: Define User Entity**

```java
package com.project.hsf.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "full_name", length = 150)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false, length = 20)
    private String role = "CUSTOMER";

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    @Column(name = "updated_date")
    private LocalDateTime updatedDate = LocalDateTime.now();
    
    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
    }
}
```

**Step 2: Create UserRepository**

```java
package com.project.hsf.repository;

import com.project.hsf.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
}
```

**Step 3: Verify Compilation**

Run: `mvn compile`
Expected: BUILD SUCCESS

**Step 4: Commit**

```bash
git add src/main/java/com/project/hsf/model/User.java src/main/java/com/project/hsf/repository/UserRepository.java
git commit -m "feat: add User entity and repository"
```

---

### Task 3: Implement KnowledgeDocument Entity

**Files:**
- Create: `src/main/java/com/project/hsf/model/KnowledgeDocument.java`
- Create: `src/main/java/com/project/hsf/repository/KnowledgeDocumentRepository.java`

**Step 1: Define KnowledgeDocument Entity**

```java
package com.project.hsf.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_documents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KnowledgeDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id")
    private Long productId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String content;

    @Column(length = 100)
    private String category;

    @Column(length = 500)
    private String keywords;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    @Column(name = "updated_date")
    private LocalDateTime updatedDate = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
    }
}
```

**Step 2: Create KnowledgeDocumentRepository**

```java
package com.project.hsf.repository;

import com.project.hsf.model.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {
    List<KnowledgeDocument> findByActiveTrue();
}
```

**Step 3: Verify Compilation**

Run: `mvn compile`
Expected: BUILD SUCCESS

**Step 4: Commit**

```bash
git add src/main/java/com/project/hsf/model/KnowledgeDocument.java src/main/java/com/project/hsf/repository/KnowledgeDocumentRepository.java
git commit -m "feat: add KnowledgeDocument entity and repository"
```

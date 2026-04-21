package com.project.hsf.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Nationalized;

import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 50)
    @NotBlank
    @NotNull
    @Nationalized
    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Email
    @Size(max = 150)
    @NotBlank
    @NotNull
    @Nationalized
    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Size(max = 255)
    @NotBlank
    @NotNull
    @Nationalized
    @Column(name = "password", nullable = false)
    private String password;

    @Size(max = 150)
    @Nationalized
    @Column(name = "full_name", length = 150)
    private String fullName;

    @Size(max = 20)
    @Pattern(regexp = "^[0-9]{9,11}$")
    @Nationalized
    @Column(name = "phone", length = 20)
    private String phone;

    @Size(max = 20)
    @Pattern(regexp = "^(ADMIN|CUSTOMER)$")
    @NotBlank
    @NotNull
    @Nationalized
    @ColumnDefault("'CUSTOMER'")
    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = false;

    @CreationTimestamp
    @ColumnDefault("getdate()")
    @Column(name = "created_date")
    private Instant createdDate;

    @UpdateTimestamp
    @ColumnDefault("getdate()")
    @Column(name = "updated_date")
    private Instant updatedDate;

}
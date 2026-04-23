package com.project.hsf.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "seafood_products", indexes = {
    @Index(name = "idx_unit", columnList = "unit")
})
public class SeafoodProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Size(max = 200)
    @NotNull
    @Nationalized
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Nationalized
    @Lob
    @Column(name = "description")
    private String description;

    @NotNull
    @Column(name = "price", nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Size(max = 20)
    @NotNull
    @Nationalized
    @ColumnDefault("'FRESH'")
    @Column(name = "freshness_status", nullable = false, length = 20)
    private String freshnessStatus;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "sold_count", nullable = false)
    private Integer soldCount;

    @Column(name = "imported_date")
    private Instant importedDate;

    @Column(name = "expiry_date")
    private Instant expiryDate;

    @Size(max = 200)
    @Nationalized
    @Column(name = "imported_from", length = 200)
    private String importedFrom;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Size(max = 20)
    @NotNull
    @Nationalized
    @Column(name = "unit", nullable = false, length = 20)
    private String unit;

    @CreationTimestamp
    @ColumnDefault("getdate()")
    @Column(name = "created_date")
    private Instant createdDate;

    @UpdateTimestamp
    @ColumnDefault("getdate()")
    @Column(name = "updated_date")
    private Instant updatedDate;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product")
    private List<ProductReview> reviews = new ArrayList<>();
}
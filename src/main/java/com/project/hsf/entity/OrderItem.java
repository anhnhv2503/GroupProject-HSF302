package com.project.hsf.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private SeafoodProduct product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combo_id")
    private Combo combo;

    @Size(max = 200)
    @Nationalized
    @Column(name = "product_name", length = 200)
    private String productName;

    @Size(max = 200)
    @Nationalized
    @Column(name = "combo_name", length = 200)
    private String comboName;

    @Size(max = 500)
    @Nationalized
    @Column(name = "product_image_url", length = 500)
    private String productImageUrl;

    @NotNull
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @NotNull
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

//    @ColumnDefault("[quantity]*[unit_price]")
    @Column(name = "subtotal", precision = 21, scale = 2)
    private BigDecimal subtotal;

    @ColumnDefault("getdate()")
    @Column(name = "created_date")
    private Instant createdDate;

}
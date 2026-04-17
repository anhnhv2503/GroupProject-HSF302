package com.project.hsf.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @NotNull
    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "shipping_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal shippingFee;

    @ColumnDefault("([total_price]-[discount_amount])+[shipping_fee]")
    @Column(name = "final_price", precision = 12, scale = 2)
    private BigDecimal finalPrice;

    @Size(max = 20)
    @NotNull
    @Nationalized
    @ColumnDefault("'PENDING'")
    @Column(name = "order_status", nullable = false, length = 20)
    private String orderStatus;

    @Size(max = 30)
    @NotNull
    @Nationalized
    @ColumnDefault("'COD'")
    @Column(name = "payment_method", nullable = false, length = 30)
    private String paymentMethod;

    @Size(max = 20)
    @NotNull
    @Nationalized
    @ColumnDefault("'UNPAID'")
    @Column(name = "payment_status", nullable = false, length = 20)
    private String paymentStatus;

    @Size(max = 500)
    @NotNull
    @Nationalized
    @Column(name = "shipping_address", nullable = false, length = 500)
    private String shippingAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_code", referencedColumnName = "code")
    private Coupon couponCode;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "notes", length = 1000)
    private String notes;

    @ColumnDefault("getdate()")
    @Column(name = "created_date")
    private Instant createdDate;

    @ColumnDefault("getdate()")
    @Column(name = "updated_date")
    private Instant updatedDate;

}
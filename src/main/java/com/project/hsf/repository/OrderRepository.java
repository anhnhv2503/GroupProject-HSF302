package com.project.hsf.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project.hsf.entity.Order;
import com.project.hsf.enums.OrderStatus;
import com.project.hsf.enums.PaymentStatus;
import com.project.hsf.entity.User;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderCode(Long orderCode);

    @Query("SELECT o FROM Order o WHERE o.customer.id = :customerId ORDER BY o.createdDate DESC")
    List<Order> findByCustomerIdOrderByCreatedDateDesc(@Param("customerId") Long customerId);


    Optional<Order> findByIdAndCustomerId(Long id, Long customerId);
    Optional<Order> findByOrderCodeAndCustomerId(Long orderCode, Long customerId);
    List<Order> findByCustomerOrderByCreatedDateDesc(User customer);

    @Query("SELECT o FROM Order o WHERE o.customer.id = :customerId AND " +
           "(:orderStatus IS NULL OR o.orderStatus = :orderStatus) AND " +
           "(:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus) AND " +
           "(:orderCode IS NULL OR CAST(o.orderCode AS string) LIKE %:orderCode%) " +
           "ORDER BY o.createdDate DESC")
    List<Order> findByCustomerIdAndFilters(
            @Param("customerId") Long customerId,
            @Param("orderStatus") OrderStatus orderStatus,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("orderCode") String orderCode);

    @Query("SELECT o FROM Order o WHERE " +
           "(:orderCode IS NULL OR CAST(o.orderCode AS string) LIKE %:orderCode%) AND " +
           "(:paymentMethod IS NULL OR o.paymentMethod = :paymentMethod) " +
           "ORDER BY o.createdDate DESC")
    List<Order> findAllWithFilters(@Param("orderCode") String orderCode, @Param("paymentMethod") String paymentMethod);

    List<Order> findTop4ByOrderByCreatedDateDesc();

    /**
     * Confirms payment exactly once.
     *
     * PayOS retries a webhook until it receives HTTP 200, so the same transaction can arrive
     * several times. The {@code paymentStatus <> PAID} condition lives inside the UPDATE itself:
     * the first delivery updates 1 row, later ones update 0. The service uses the affected row
     * count to decide whether to run side effects (writing history, crediting revenue).
     *
     * Check-then-write (findById -> if -> save) is not safe here: two concurrent webhooks would
     * both read UNPAID before either one writes, and both would run the side effects.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Order o SET o.paymentStatus = com.project.hsf.enums.PaymentStatus.PAID, "
            + "o.orderStatus = com.project.hsf.enums.OrderStatus.CONFIRMED, o.updatedDate = :now "
            + "WHERE o.orderCode = :orderCode "
            + "AND o.paymentStatus <> com.project.hsf.enums.PaymentStatus.PAID "
            + "AND o.orderStatus <> com.project.hsf.enums.OrderStatus.CANCELLED")
    int markPaidIfUnpaid(@Param("orderCode") Long orderCode, @Param("now") Instant now);

    /**
     * Cancels an order only while it is still pending and unpaid. Shared by the customer-cancel
     * flow and the expiry job, so when both run at once only one of them actually cancels — the
     * other updates 0 rows and skips, which prevents stock being returned twice for one order.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Order o SET o.orderStatus = com.project.hsf.enums.OrderStatus.CANCELLED, "
            + "o.paymentStatus = com.project.hsf.enums.PaymentStatus.CANCELLED, o.updatedDate = :now "
            + "WHERE o.id = :id "
            + "AND o.orderStatus = com.project.hsf.enums.OrderStatus.PENDING "
            + "AND o.paymentStatus <> com.project.hsf.enums.PaymentStatus.PAID")
    int cancelIfStillPending(@Param("id") Long id, @Param("now") Instant now);

    /**
     * Bank-transfer orders past their payment window: the PayOS link only lives 30 minutes, but
     * stock was deducted at placement time. Without cleanup those units stay reserved forever.
     */
    @Query("SELECT o FROM Order o WHERE o.orderStatus = com.project.hsf.enums.OrderStatus.PENDING "
            + "AND o.paymentStatus <> com.project.hsf.enums.PaymentStatus.PAID "
            + "AND o.paymentMethod = :paymentMethod AND o.createdDate < :cutoff")
    List<Order> findExpiredUnpaidOrders(@Param("paymentMethod") String paymentMethod,
            @Param("cutoff") Instant cutoff);

    /** The admin order list reads order.customer on every row, so fetch-join it to avoid N+1. */
    @Override
    @EntityGraph(attributePaths = { "customer" })
    List<Order> findAll(Sort sort);
}

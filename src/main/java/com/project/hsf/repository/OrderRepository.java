package com.project.hsf.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project.hsf.entity.Order;
import com.project.hsf.entity.OrderStatus;
import com.project.hsf.entity.PaymentStatus;
import com.project.hsf.entity.User;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderCode(Long orderCode);

    @Query("SELECT o FROM Order o WHERE o.customer.id = :customerId ORDER BY o.createdDate DESC")
    List<Order> findByCustomerIdOrderByCreatedDateDesc(@Param("customerId") Long customerId);

    @Query("SELECT o FROM Order o WHERE o.customer.id = :customerId AND " +
           "(:orderStatus IS NULL OR o.orderStatus = :orderStatus) AND " +
           "(:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus) " +
           "ORDER BY o.createdDate DESC")
    List<Order> findByCustomerIdAndFilters(
            @Param("customerId") Long customerId,
            @Param("orderStatus") OrderStatus orderStatus,
            @Param("paymentStatus") PaymentStatus paymentStatus);

    Optional<Order> findByIdAndCustomerId(Long id, Long customerId);
    List<Order> findByCustomerOrderByCreatedDateDesc(User customer);

    long countByCreatedDateBetween(Instant start, Instant end);

    long countByCreatedDateBetweenAndPaymentStatus(Instant start, Instant end, PaymentStatus paymentStatus);

    @Query("SELECT SUM(COALESCE(o.finalPrice, o.totalPrice)) FROM Order o WHERE o.createdDate >= :start AND o.createdDate < :end AND o.orderStatus <> :excludedStatus AND o.paymentStatus = :paymentStatus")
    BigDecimal sumRevenueBetween(@Param("start") Instant start,
                                @Param("end") Instant end,
                                @Param("excludedStatus") OrderStatus excludedStatus,
                                @Param("paymentStatus") PaymentStatus paymentStatus);

    default BigDecimal sumRevenueBetween(Instant start, Instant end) {
        return sumRevenueBetween(start, end, OrderStatus.CANCELLED, PaymentStatus.PAID);
    }

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.customer ORDER BY o.createdDate DESC")
    List<Order> findRecentOrdersWithCustomer(Pageable pageable);
}

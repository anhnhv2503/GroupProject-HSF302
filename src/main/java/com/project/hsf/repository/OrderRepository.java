package com.project.hsf.repository;

import com.project.hsf.entity.Order;
import com.project.hsf.entity.User;
import com.project.hsf.enums.OrderStatus;
import com.project.hsf.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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

    List<Order> findTop4ByOrderByCreatedDateDesc();

}

package com.project.hsf.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project.hsf.entity.Order;
import com.project.hsf.entity.OrderStatus;
import com.project.hsf.entity.PaymentStatus;
import com.project.hsf.entity.User;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

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
}

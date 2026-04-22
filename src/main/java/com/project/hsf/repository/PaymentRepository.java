package com.project.hsf.repository;

import com.project.hsf.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = :status")
    Long sumAmountByStatus(String status);
}

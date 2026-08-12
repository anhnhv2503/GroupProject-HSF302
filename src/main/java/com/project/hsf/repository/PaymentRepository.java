package com.project.hsf.repository;

import com.project.hsf.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    /**
     * Looks up a payment by its PayOS transaction reference. Used for manual reconciliation:
     * given a reference on a bank statement, find the matching order.
     */
    Optional<Payment> findByTransferRef(String transferRef);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = :status")
    Long sumAmountByStatus(String status);

    List<Payment> findByStatus(String status);
}

package com.project.hsf.service.impl;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.hsf.entity.Order;
import com.project.hsf.entity.OrderStatusHistory;
import com.project.hsf.entity.Payment;
import com.project.hsf.enums.OrderStatus;
import com.project.hsf.enums.PaymentStatus;
import com.project.hsf.enums.PaymentWebhookResult;
import com.project.hsf.repository.OrderRepository;
import com.project.hsf.repository.OrderStatusHistoryRepository;
import com.project.hsf.repository.PaymentRepository;
import com.project.hsf.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;
import vn.payos.model.v2.paymentRequests.Transaction;
import vn.payos.model.webhooks.WebhookData;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    /** The code PayOS returns for a successful transaction. */
    private static final String PAYOS_SUCCESS_CODE = "00";

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final PayOS payOS;

    @Override
    @Transactional
    public PaymentWebhookResult confirmPayment(WebhookData data) {
        Long orderCode = data.getOrderCode();

        Order order = orderRepository.findByOrderCode(orderCode).orElse(null);
        if (order == null) {
            log.warn("Webhook for unknown orderCode: {}", orderCode);
            return PaymentWebhookResult.ORDER_NOT_FOUND;
        }

        if (!PAYOS_SUCCESS_CODE.equals(data.getCode())) {
            log.info("Webhook reports a failed transaction. orderCode={}, code={}, desc={}",
                    orderCode, data.getCode(), data.getDesc());
            return PaymentWebhookResult.PAYMENT_FAILED;
        }

        // A valid signature only proves the payload came from PayOS; it does not prove the amount
        // matches this order. Still compare against the finalPrice computed on the server.
        long expected = order.getFinalPrice() != null ? order.getFinalPrice().longValue() : 0L;
        long actual = data.getAmount() != null ? data.getAmount() : 0L;
        if (expected != actual) {
            log.error("Amount mismatch: orderCode={}, order={}, webhook={}. Payment not accepted.",
                    orderCode, expected, actual);
            return PaymentWebhookResult.AMOUNT_MISMATCH;
        }

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            log.error("Money arrived for a cancelled order: orderCode={}. Manual refund needed.", orderCode);
            return PaymentWebhookResult.ORDER_ALREADY_CANCELLED;
        }

        // This is where idempotency is enforced: the "not yet PAID" condition is part of the UPDATE,
        // so of two concurrent webhooks only one updates a row. The other gets 0 and returns here,
        // before it can write history or credit revenue a second time.
        int updated = orderRepository.markPaidIfUnpaid(orderCode, Instant.now());
        if (updated == 0) {
            log.info("Duplicate webhook for orderCode={}, already processed. Skipping.", orderCode);
            return PaymentWebhookResult.ALREADY_PROCESSED;
        }

        recordPaymentDetails(order, data.getReference(), data.getTransactionDateTime());
        writeHistory(order, "PayOS payment succeeded. Transaction reference: " + data.getReference());

        log.info("Payment accepted for orderCode={}, reference={}", orderCode, data.getReference());
        return PaymentWebhookResult.CONFIRMED;
    }

    @Override
    @Transactional
    public Order reconcileOrderWithProvider(Long orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode).orElse(null);
        if (order == null) {
            return null;
        }

        // Nothing to reconcile once the order has reached a final state.
        if (order.getPaymentStatus() == PaymentStatus.PAID
                || order.getOrderStatus() == OrderStatus.CANCELLED) {
            return order;
        }

        PaymentLink link;
        try {
            link = payOS.paymentRequests().get(orderCode);
        } catch (Exception e) {
            // PayOS is down or timed out: leave the order pending and let the webhook settle it.
            // "Unpaid" must never be inferred from a network error.
            log.warn("Could not reconcile orderCode={} with PayOS: {}", orderCode, e.getMessage());
            return order;
        }

        if (link == null || link.getStatus() != PaymentLinkStatus.PAID) {
            return order;
        }

        long expected = order.getFinalPrice() != null ? order.getFinalPrice().longValue() : 0L;
        long paid = link.getAmountPaid() != null ? link.getAmountPaid() : 0L;
        if (expected != paid) {
            log.error("Reconciliation amount mismatch: orderCode={}, order={}, paid={}",
                    orderCode, expected, paid);
            return order;
        }

        int updated = orderRepository.markPaidIfUnpaid(orderCode, Instant.now());
        if (updated == 0) {
            // A webhook settled it between our two statements - re-read to return the real state.
            return orderRepository.findByOrderCode(orderCode).orElse(order);
        }

        String reference = firstTransactionReference(link);
        recordPaymentDetails(order, reference, null);
        writeHistory(order, "Payment confirmed by direct reconciliation with PayOS.");

        return orderRepository.findByOrderCode(orderCode).orElse(order);
    }

    private String firstTransactionReference(PaymentLink link) {
        if (link.getTransactions() == null || link.getTransactions().isEmpty()) {
            return null;
        }
        Transaction first = link.getTransactions().get(0);
        return first != null ? first.getReference() : null;
    }

    /**
     * Stores the PayOS transaction reference so the payment can later be matched against a bank statement.
     */
    private void recordPaymentDetails(Order order, String reference, String transactionDateTime) {
        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        if (payment == null) {
            return;
        }
        payment.setStatus(PaymentStatus.PAID.name());
        payment.setTransferRef(reference);
        payment.setTransferredAt(Instant.now());
        payment.setConfirmedBy("PAYOS_WEBHOOK");
        payment.setConfirmedAt(Instant.now());
        if (transactionDateTime != null) {
            payment.setNote("PayOS transactionDateTime: " + transactionDateTime);
        }
        payment.setUpdatedDate(Instant.now());
        paymentRepository.save(payment);
    }

    private void writeHistory(Order order, String note) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(OrderStatus.CONFIRMED);
        history.setChangedBy("SYSTEM");
        history.setChangedAt(Instant.now());
        history.setNote(note);
        orderStatusHistoryRepository.save(history);
    }
}

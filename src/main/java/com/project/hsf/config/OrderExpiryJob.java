package com.project.hsf.config;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.project.hsf.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Cleans up bank-transfer orders whose payment window has expired.
 *
 * Stock is deducted at order placement to prevent overselling, but a PayOS link only lives 30
 * minutes. If a customer places an order and never pays, those units stay reserved indefinitely -
 * sold out on the site while still sitting in the warehouse. This job gives them back.
 *
 * It runs every 5 minutes rather than every minute: an already-expired order is not urgent, and the
 * looser interval means far fewer scans of the orders table when there is nothing to clean up.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderExpiryJob {

    private final OrderService orderService;

    @Scheduled(fixedDelayString = "${app.order-expiry.interval-ms:300000}", initialDelay = 60_000)
    public void cancelExpiredUnpaidOrders() {
        try {
            int cancelled = orderService.expireUnpaidOrders();
            if (cancelled > 0) {
                log.info("Cancelled {} expired unpaid orders and returned their stock.", cancelled);
            }
        } catch (Exception e) {
            // An exception escaping a fixedDelay job makes Spring stop scheduling it, so catch here
            // to keep one bad run from killing the schedule permanently.
            log.error("Failed to clean up expired orders: {}", e.getMessage(), e);
        }
    }
}

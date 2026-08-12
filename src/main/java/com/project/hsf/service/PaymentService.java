package com.project.hsf.service;

import com.project.hsf.entity.Order;
import com.project.hsf.enums.PaymentWebhookResult;

import vn.payos.model.webhooks.WebhookData;

public interface PaymentService {

    /**
     * Accepts payment from a webhook whose signature has ALREADY been verified.
     *
     * Idempotent by orderCode: calling it repeatedly with the same transaction only has an effect
     * the first time. The caller must verify the signature first; this service deliberately does
     * not, so that authentication (controller) and business rules (service) stay separate.
     */
    PaymentWebhookResult confirmPayment(WebhookData data);

    /**
     * Asks PayOS directly whether an order has been paid, instead of trusting a redirect URL parameter.
     *
     * Used when the customer returns from the payment page: the browser could have tampered with the
     * parameters, or the webhook may not have arrived yet. This is a server-to-server call, so it
     * cannot be spoofed.
     */
    Order reconcileOrderWithProvider(Long orderCode);
}

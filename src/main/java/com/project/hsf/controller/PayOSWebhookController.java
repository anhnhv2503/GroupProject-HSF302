package com.project.hsf.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.hsf.enums.PaymentWebhookResult;
import com.project.hsf.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.payos.PayOS;
import vn.payos.model.webhooks.WebhookData;

/**
 * The only place an order can be marked as paid.
 *
 * Previously PAID was set from a parameter on the browser redirect URL
 * (/checkout/callback?status=PAID), which meant anyone could mark their own order as paid. The
 * redirect is now display-only; PAID is written here after the PayOS HMAC signature is verified,
 * or through the server-to-server reconciliation path.
 */
@RestController
@RequestMapping("/api/payments/payos")
@RequiredArgsConstructor
@Slf4j
public class PayOSWebhookController {

    private final PayOS payOS;
    private final PaymentService paymentService;

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> receiveWebhook(@RequestBody Object payload) {
        WebhookData data;
        try {
            // verify() recomputes the HMAC from the checksumKey and compares it with the signature
            // field in the payload. A bad signature means the request is not from PayOS, so we do
            // not read its contents and do not write anything.
            data = payOS.webhooks().verify(payload);
        } catch (Exception e) {
            log.warn("Webhook rejected, invalid signature: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid signature"));
        }

        PaymentWebhookResult result = paymentService.confirmPayment(data);

        // PayOS retries until it gets a 2xx, so return 200 for the "nothing to do" outcomes too
        // (duplicate, unknown order, failed transaction) - those are final states and retrying
        // changes nothing. Only answer non-2xx when a retry is genuinely wanted.
        boolean acknowledged = result != PaymentWebhookResult.AMOUNT_MISMATCH;
        Map<String, Object> body = Map.of(
                "success", result == PaymentWebhookResult.CONFIRMED
                        || result == PaymentWebhookResult.ALREADY_PROCESSED,
                "result", result.name());

        if (!acknowledged) {
            // Amount mismatch: deliberately not 200, so it leaves a trace in PayOS's retry log
            // while still refusing to accept the payment automatically.
            return ResponseEntity.unprocessableEntity().body(body);
        }
        return ResponseEntity.ok(body);
    }
}

package com.project.hsf.enums;

/**
 * Outcome of processing one payment webhook.
 *
 * This is an enum rather than a boolean because the controller has to distinguish two cases that
 * are very different in business terms but both answer HTTP 200 to PayOS:
 * - {@link #CONFIRMED}: first delivery, payment accepted.
 * - {@link #ALREADY_PROCESSED}: a retry, nothing more to do.
 *
 * Both must return 200 so PayOS stops retrying. {@link #AMOUNT_MISMATCH} must not be silently
 * swallowed, though — it means the data disagrees and a human has to reconcile it.
 */
public enum PaymentWebhookResult {

    /** First time we saw this transaction: the order moved to PAID + CONFIRMED. */
    CONFIRMED,

    /** Already handled earlier (retried webhook) — skipped, side effects not run again. */
    ALREADY_PROCESSED,

    /** No order matches the given orderCode. */
    ORDER_NOT_FOUND,

    /** The webhook amount does not match the order total — payment refused. */
    AMOUNT_MISMATCH,

    /** PayOS reported a failed transaction (code other than "00"). */
    PAYMENT_FAILED,

    /** The order was cancelled before the money arrived — needs a manual refund. */
    ORDER_ALREADY_CANCELLED
}

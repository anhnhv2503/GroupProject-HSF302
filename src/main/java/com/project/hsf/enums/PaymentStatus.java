package com.project.hsf.enums;

public enum PaymentStatus {
    UNPAID("Chưa thanh toán"),
    PAID("Đã thanh toán"),
    REFUNDED("Đã hoàn tiền"),
    PENDING("Đang chờ"),
    FAILED("Thất bại"),
    CANCELLED("Đã hủy");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
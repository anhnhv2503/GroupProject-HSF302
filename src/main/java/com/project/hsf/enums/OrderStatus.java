package com.project.hsf.enums;

public enum OrderStatus {
    PENDING("Chờ xác nhận"),
    CONFIRMED("Đã xác nhận"),
    PROCESSING("Đang xử lý"),
    SHIPPED("Đã bàn giao"),
    SHIPPING("Đang giao"),
    DELIVERED("Giao thành công"),
    CANCELLED("Đã hủy");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
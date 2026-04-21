package com.project.hsf.entity;

public enum OrderStatus {
    PENDING("Chờ xác nhận"),
    CONFIRMED("Đã xác nhận"),
    PROCESSING("Đang xử lý"),
    SHIPPED("Đã giao hàng"),
    SHIPPING("Đang giao"),
    DELIVERED("Đã giao"),
    CANCELLED("Đã hủy");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
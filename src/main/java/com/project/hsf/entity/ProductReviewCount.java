package com.project.hsf.entity;

public class ProductReviewCount {
    private final Long productId;
    private final Long count;

    public ProductReviewCount(Long productId, Long count) {
        this.productId = productId;
        this.count = count;
    }

    public Long getProductId() {
        return productId;
    }

    public Long getCount() {
        return count;
    }
}
package com.project.hsf.service;

import com.project.hsf.entity.Order;
import com.project.hsf.entity.ProductReview;
import com.project.hsf.entity.ProductReviewCount;
import com.project.hsf.entity.SeafoodProduct;
import com.project.hsf.entity.User;

import java.util.List;

public interface ProductReviewService {
    List<ProductReview> getReviewsByProduct(Long productId);

    List<ProductReview> getVisibleReviewsByProduct(Long productId);

    ProductReview getReviewById(Long id);

    ProductReview createReview(SeafoodProduct product, User user, Order order, Short rating, String comment);

    ProductReview updateReview(Long id, Short rating, String comment);

    void deleteReview(Long id);

    boolean hasUserReviewedProduct(Long productId, Long userId);

    boolean canUserReviewProduct(Long productId, Long userId);

    List<ProductReview> getUserReviews(Long userId);

    ProductReview getUserReviewForProduct(Long productId, Long userId);

    List<ProductReview> getPendingReviews();

    long getPendingReviewCount();

    ProductReview approveReview(Long id);

    List<ProductReview> getAllReviewsForProduct(Long productId);

    List<ProductReviewCount> getPendingReviewCountByProduct();
}
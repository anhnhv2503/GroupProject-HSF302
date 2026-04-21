package com.project.hsf.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.hsf.entity.Order;
import com.project.hsf.entity.OrderStatus;
import com.project.hsf.entity.ProductReview;
import com.project.hsf.entity.SeafoodProduct;
import com.project.hsf.entity.User;
import com.project.hsf.repository.OrderRepository;
import com.project.hsf.repository.ProductReviewRepository;
import com.project.hsf.repository.UserRepository;
import com.project.hsf.service.ProductReviewService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductReviewServiceImpl implements ProductReviewService {

    private final ProductReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductReview> getReviewsByProduct(Long productId) {
        return reviewRepository.findByProductId(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductReview> getVisibleReviewsByProduct(Long productId) {
        return reviewRepository.findByProductIdAndIsVisibleTrue(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductReview getReviewById(Long id) {
        return reviewRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public ProductReview createReview(SeafoodProduct product, User user, Order order, Short rating, String comment) {
        ProductReview review = ProductReview.builder()
                .product(product)
                .user(user)
                .order(order)
                .rating(rating)
                .comment(comment)
                .isVisible(false)
                .build();
        return reviewRepository.save(review);
    }

    @Override
    @Transactional
    public ProductReview updateReview(Long id, Short rating, String comment) {
        ProductReview review = reviewRepository.findById(id).orElse(null);
        if (review != null) {
            review.setRating(rating);
            review.setComment(comment);
            return reviewRepository.save(review);
        }
        return null;
    }

    @Override
    @Transactional
    public void deleteReview(Long id) {
        reviewRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUserReviewedProduct(Long productId, Long userId) {
        return reviewRepository.existsByProductIdAndUserId(productId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserReviewProduct(Long productId, Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return false;
        List<Order> orders = orderRepository.findByCustomerOrderByCreatedDateDesc(user);
        return orders.stream()
                .anyMatch(order -> order.getOrderStatus() == OrderStatus.DELIVERED &&
                        order.getOrderItems().stream()
                                .anyMatch(item -> item.getProduct() != null &&
                                        item.getProduct().getId().equals(productId)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductReview> getUserReviews(Long userId) {
        return reviewRepository.findAll().stream()
                .filter(r -> r.getUser().getId().equals(userId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductReview getUserReviewForProduct(Long productId, Long userId) {
        return reviewRepository.findAll().stream()
                .filter(r -> r.getProduct().getId().equals(productId) && r.getUser().getId().equals(userId))
                .findFirst()
                .orElse(null);
    }
}
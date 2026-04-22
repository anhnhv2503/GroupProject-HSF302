package com.project.hsf.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.hsf.entity.ProductReview;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
    List<ProductReview> findByProductId(Long productId);

    List<ProductReview> findByProductIdAndIsVisibleTrue(Long productId);

    Optional<ProductReview> findByProductIdAndUserId(Long productId, Long userId);

    boolean existsByProductIdAndUserId(Long productId, Long userId);

    List<ProductReview> findByIsVisibleFalse();

    long countByIsVisibleFalse();
}
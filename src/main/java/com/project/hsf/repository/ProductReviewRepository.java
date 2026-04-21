package com.project.hsf.repository;

import com.project.hsf.entity.ProductReview;
import com.project.hsf.entity.SeafoodProduct;
import com.project.hsf.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
    List<ProductReview> findByProductId(Long productId);

    List<ProductReview> findByProductIdAndIsVisibleTrue(Long productId);

    Optional<ProductReview> findByProductIdAndUserId(Long productId, Long userId);

    boolean existsByProductIdAndUserId(Long productId, Long userId);
}
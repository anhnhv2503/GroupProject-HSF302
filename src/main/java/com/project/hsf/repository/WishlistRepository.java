package com.project.hsf.repository;

import com.project.hsf.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    List<Wishlist> findByUserUsernameOrderByAddedDateDesc(String username);
    Optional<Wishlist> findByUserUsernameAndProductId(String username, Long productId);
    long countByUserUsername(String username);
    boolean existsByUserUsernameAndProductId(String username, Long productId);
}

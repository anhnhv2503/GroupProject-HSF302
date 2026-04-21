package com.project.hsf.service.impl;

import com.project.hsf.entity.SeafoodProduct;
import com.project.hsf.entity.User;
import com.project.hsf.entity.Wishlist;
import com.project.hsf.repository.SeafoodProductRepository;
import com.project.hsf.repository.UserRepository;
import com.project.hsf.repository.WishlistRepository;
import com.project.hsf.service.WishlistService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final SeafoodProductRepository productRepository;

    @Override
    @Transactional
    public Map<String, Object> toggleWishlist(String username, Long productId, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        User user = userRepository.findByUsername(username);
        if (user == null) {
            response.put("error", "User not found");
            return response;
        }

        wishlistRepository.findByUserUsernameAndProductId(username, productId).ifPresentOrElse(
            wishlist -> {
                wishlistRepository.delete(wishlist);
                response.put("added", false);
            },
            () -> {
                SeafoodProduct product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found"));
                Wishlist wishlist = new Wishlist();
                wishlist.setUser(user);
                wishlist.setProduct(product);
                wishlist.setAddedDate(Instant.now());
                wishlistRepository.save(wishlist);
                response.put("added", true);
            }
        );

        updateSessionCounters(username, session);
        response.put("count", wishlistRepository.countByUserUsername(username));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeafoodProduct> getWishlistProducts(String username) {
        return wishlistRepository.findByUserUsernameOrderByAddedDateDesc(username)
                .stream()
                .map(Wishlist::getProduct)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public void updateSessionCounters(String username, HttpSession session) {
        long count = wishlistRepository.countByUserUsername(username);
        Set<Long> productIds = wishlistRepository.findByUserUsernameOrderByAddedDateDesc(username)
                .stream()
                .map(w -> w.getProduct().getId())
                .collect(Collectors.toSet());
        
        session.setAttribute("wishlistCount", (int) count);
        session.setAttribute("wishlistProductIds", productIds);
    }
}

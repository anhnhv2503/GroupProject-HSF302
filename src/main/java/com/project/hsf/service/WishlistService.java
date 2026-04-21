package com.project.hsf.service;

import com.project.hsf.entity.SeafoodProduct;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

public interface WishlistService {
    Map<String, Object> toggleWishlist(String username, Long productId, HttpSession session);
    List<SeafoodProduct> getWishlistProducts(String username);
    void updateSessionCounters(String username, HttpSession session);
}

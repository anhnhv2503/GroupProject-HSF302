package com.project.hsf.controller;

import com.project.hsf.entity.SeafoodProduct;
import com.project.hsf.service.WishlistService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public String viewWishlist(Principal principal, Model model, HttpSession session) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        String username = principal.getName();
        List<SeafoodProduct> products = wishlistService.getWishlistProducts(username);
        
        // Initial sync of session counters on page load
        wishlistService.updateSessionCounters(username, session);
        
        model.addAttribute("products", products);
        return "user/wishlist.html";
    }

    @PostMapping("/toggle/{productId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleWishlist(
            @PathVariable Long productId,
            Principal principal,
            HttpSession session) {
        
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        Map<String, Object> result = wishlistService.toggleWishlist(principal.getName(), productId, session);
        return ResponseEntity.ok(result);
    }
}

package com.project.hsf.controller.client;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.project.hsf.entity.SeafoodProduct;
import com.project.hsf.entity.User;
import com.project.hsf.service.UserService;
import com.project.hsf.service.WishlistService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;
    private final UserService userService;

    @GetMapping
    public String viewWishlist(Principal principal, Model model, HttpSession session, Authentication authentication) {
        if (principal == null) {
            return "redirect:/login";
        }

        String username = principal.getName();
        List<SeafoodProduct> products = wishlistService.getWishlistProducts(username);

        wishlistService.updateSessionCounters(username, session);

        if (authentication != null && authentication.isAuthenticated()) {
            User user = userService.findByUsername(username);
            if (user != null) {
                model.addAttribute("canAddToCart", userService.canUserAddToCart(user.getId()));
            }
        }

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

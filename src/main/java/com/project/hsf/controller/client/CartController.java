package com.project.hsf.controller.client;

import com.project.hsf.dto.CartItemDTO;
import com.project.hsf.entity.User;
import com.project.hsf.service.CartService;
import com.project.hsf.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final UserService userService;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return userService.findByUsername(auth.getName());
        }
        return null;
    }

    @GetMapping
    public String viewCart(HttpSession session, Model model) {
        User currentUser = getCurrentUser();
        boolean canAddToCart = true;
        if (currentUser != null) {
            canAddToCart = userService.canUserAddToCart(currentUser.getId());
        }

        model.addAttribute("cartItems", cartService.getCart(session).values());
        model.addAttribute("totalPrice", cartService.calculateTotal(session));
        model.addAttribute("canAddToCart", canAddToCart);
        return "cart/cart";
    }

    /**
     * Add item to cart.
     * Either productId or comboId must be provided (not both).
     */
    @PostMapping("/add")
    public Object addToCart(
            HttpServletRequest request,
            HttpSession session,
            @RequestParam(required = false) Integer productId,
            @RequestParam(required = false) Integer comboId,
            @RequestParam String name,
            @RequestParam Double price,
            @RequestParam(defaultValue = "1") Integer quantity,
            @RequestParam(required = false) String imageUrl) {

        User currentUser = getCurrentUser();
        if (currentUser != null && !userService.canUserAddToCart(currentUser.getId())) {
            String errorMsg = "Tài khoản của bạn bị hạn chế mua hàng. Vui lòng liên hệ admin để được hỗ trợ.";
            if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                return ResponseEntity.badRequest().body(Map.of("error", errorMsg));
            }
            return "redirect:/cart?error="
                    + java.net.URLEncoder.encode(errorMsg, java.nio.charset.StandardCharsets.UTF_8);
        }

        // Validate: must have either productId or comboId
        if (productId == null && comboId == null) {
            String errorMsg = "invalid";
            if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                return ResponseEntity.badRequest().body(Map.of("error", errorMsg));
            }
            return "redirect:/cart?error=" + errorMsg;
        }

        String key = (productId != null) ? "P_" + productId : "C_" + comboId;
        String type = (productId != null) ? "PRODUCT" : "COMBO";

        CartItemDTO item = CartItemDTO.builder()
                .itemKey(key)
                .productId(productId)
                .comboId(comboId)
                .name(name)
                .unitPrice(price)
                .quantity(quantity)
                .imageUrl(imageUrl != null ? imageUrl : "")
                .itemType(type)
                .build();

        cartService.addToCart(session, item);

        // Update session cart count for navbar badge
        int count = cartService.getCart(session).values()
                .stream().mapToInt(CartItemDTO::getQuantity).sum();
        session.setAttribute("cartCount", count);

        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            return ResponseEntity.ok(Map.of("success", true, "count", count));
        }

        return "redirect:/cart?added=true";
    }

    @PostMapping("/update")
    public String updateQuantity(
            HttpSession session,
            @RequestParam String itemKey,
            @RequestParam Integer quantity,
            Model model) {

        try {
            cartService.updateQuantity(session, itemKey, quantity);
            session.setAttribute("cartCount", cartService.getCart(session).values()
                    .stream().mapToInt(CartItemDTO::getQuantity).sum());

            return "redirect:/cart";

        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/cart?error=" + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }

        // Refresh cart count

    }

    @PostMapping("/remove")
    public String removeFromCart(HttpSession session, @RequestParam String itemKey) {
        cartService.removeItem(session, itemKey);

        // Refresh cart count
        session.setAttribute("cartCount", cartService.getCart(session).values()
                .stream().mapToInt(CartItemDTO::getQuantity).sum());

        return "redirect:/cart";
    }
}
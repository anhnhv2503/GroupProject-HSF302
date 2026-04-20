package com.project.hsf.controller;

import com.project.hsf.dto.CartItemDTO;
import com.project.hsf.service.CartService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public String viewCart(HttpSession session, Model model) {
        model.addAttribute("cartItems", cartService.getCart(session).values());
        model.addAttribute("totalPrice", cartService.calculateTotal(session));
        return "cart/cart";
    }

    @PostMapping("/add")
    public String addToCart(HttpSession session,
                            @RequestParam(required = false) Integer productId,
                            @RequestParam(required = false) Integer comboId,
                            @RequestParam String name,
                            @RequestParam Double price,
                            @RequestParam(defaultValue = "1") Integer quantity) {
        
        String key = (productId != null) ? "P_" + productId : "C_" + comboId;
        CartItemDTO item = CartItemDTO.builder()
                .itemKey(key)
                .productId(productId)
                .comboId(comboId)
                .name(name)
                .unitPrice(price)
                .quantity(quantity)
                .build();
                
        cartService.addToCart(session, item);
        return "redirect:/cart?added=true";
    }

    @PostMapping("/update")
    public String updateQuantity(HttpSession session,
                                 @RequestParam String itemKey,
                                 @RequestParam Integer quantity) {
        cartService.updateQuantity(session, itemKey, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/remove")
    public String removeFromCart(HttpSession session, @RequestParam String itemKey) {
        cartService.removeItem(session, itemKey);
        return "redirect:/cart";
    }
}

package com.project.hsf.controller;

import com.project.hsf.dto.CartItemDTO;
import com.project.hsf.service.CartService;
import com.project.hsf.service.OrderService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CartService cartService;
    private final OrderService orderService;

    @GetMapping
    public String showCheckoutPage(HttpSession session, Model model) {
        var cart = cartService.getCart(session);
        if (cart.isEmpty()) {
            return "redirect:/cart";
        }
        model.addAttribute("cartItems", cart.values());
        model.addAttribute("totalPrice", cartService.calculateTotal(session));
        return "checkout/checkout";
    }

    @PostMapping("/place-order")
    public String placeOrder(HttpSession session, 
                             @RequestParam(required = false) String couponCode,
                             @RequestParam String address,
                             @RequestParam String paymentMethod,
                             Model model) {
        List<CartItemDTO> cartItems = new ArrayList<>(cartService.getCart(session).values());
        if (cartItems.isEmpty()) {
            return "redirect:/cart";
        }

        try {
            // Simplified order creation logic call
            orderService.placeOrder(cartItems, couponCode, null);
            
            // Clear cart upon successful transaction
            cartService.clearCart(session);
            return "redirect:/checkout?success=true";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("cartItems", cartItems);
            model.addAttribute("totalPrice", cartService.calculateTotal(session));
            return "checkout/checkout";
        }
    }
}

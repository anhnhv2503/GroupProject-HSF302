package com.project.hsf.controller;

import com.project.hsf.dto.CartItemDTO;
import com.project.hsf.entity.Coupon;
import com.project.hsf.entity.User;
import com.project.hsf.repository.CouponRepository;
import com.project.hsf.repository.UserAddressRepository;
import com.project.hsf.repository.UserRepository;
import com.project.hsf.service.CartService;
import com.project.hsf.service.OrderService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CartService cartService;
    private final OrderService orderService;
    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;
    private final CouponRepository couponRepository;

    @GetMapping
    public String showCheckoutPage(HttpSession session, Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        var cart = cartService.getCart(session);
        if (cart.isEmpty()) {
            return "redirect:/cart";
        }

        User user = userRepository.findByUsername(principal.getName());
        var addresses = userAddressRepository.findByUser(user);

        String appliedCouponCode = (String) session.getAttribute("appliedCoupon");
        double totalPrice = cartService.calculateTotal(session);
        double discountAmount = 0;

//        if (appliedCouponCode != null) {
//            Coupon coupon = couponRepository.findByCode(appliedCouponCode).orElse(null);
//            if (coupon != null && Boolean.TRUE.equals(coupon.getIsActive())) {
//                if ("PERCENTAGE".equalsIgnoreCase(coupon.getDiscountType())) {
//                    discountAmount = totalPrice * (coupon.getDiscountValue() / 100.0);
//                } else {
//                    discountAmount = coupon.getDiscountValue();
//                }
//            }
//        }
        
        model.addAttribute("cartItems", cart.values());
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("discountAmount", discountAmount);
        model.addAttribute("addresses", addresses);
        model.addAttribute("appliedCoupon", appliedCouponCode);
        
        // If there's a success param from redirect
        if (session.getAttribute("orderSuccess") != null) {
            model.addAttribute("success", "Đặt hàng thành công!");
            session.removeAttribute("orderSuccess");
        }

        return "checkout/checkout";
    }

    @PostMapping("/apply-coupon")
    public String applyCoupon(@RequestParam String code, HttpSession session) {
        // We just store it in session, validation happens during placeOrder or we could validate here
        session.setAttribute("appliedCoupon", code.toUpperCase());
        return "redirect:/checkout";
    }

    @PostMapping("/place-order")
    public String placeOrder(HttpSession session, 
                             Principal principal,
                             @RequestParam String address,
                             @RequestParam String paymentMethod,
                             @RequestParam(required = false) String notes,
                             Model model) {
        
        if (principal == null) {
            return "redirect:/login";
        }

        String couponCode = (String) session.getAttribute("appliedCoupon");

        List<CartItemDTO> cartItems = new ArrayList<>(cartService.getCart(session).values());
        if (cartItems.isEmpty()) {
            return "redirect:/cart";
        }

        User user = userRepository.findByUsername(principal.getName());

        try {
            orderService.placeOrder(cartItems, couponCode, address, paymentMethod, notes, user);
            
            // Clear cart and coupon upon successful transaction
            cartService.clearCart(session);
            session.removeAttribute("appliedCoupon");
            session.setAttribute("orderSuccess", true);
            
            return "redirect:/checkout?success=true";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("cartItems", cartItems);
            model.addAttribute("totalPrice", cartService.calculateTotal(session));
            model.addAttribute("addresses", userAddressRepository.findByUser(user));
            model.addAttribute("appliedCoupon", couponCode);
            return "checkout/checkout";
        }
    }
}

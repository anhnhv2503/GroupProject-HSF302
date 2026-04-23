package com.project.hsf.controller.client;

import com.project.hsf.dto.CartItemDTO;
import com.project.hsf.entity.Coupon;
import com.project.hsf.entity.Order;
import com.project.hsf.entity.User;
import com.project.hsf.service.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CartService cartService;
    private final OrderService orderService;
    private final UserAddressService userAddressService;

    private final CouponService couponService;
    private final UserService userService;

    @GetMapping
    public String showCheckoutPage(HttpSession session, Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        User user = userService.findByUsername(principal.getName());
        if (!userService.canUserAddToCart(user.getId())) {
            model.addAttribute("error", "Quyền mua hàng của bạn đã bị thu hồi. Liên hệ admin.");
            model.addAttribute("cartItems", cartService.getCart(session).values());
            model.addAttribute("totalPrice", cartService.calculateTotal(session));
            return "cart/cart";
        }

        var cart = cartService.getCart(session);
        if (cart.isEmpty()) {
            return "redirect:/cart";
        }

        var addresses = userAddressService.findByUser(user);

        String appliedCouponCode = (String) session.getAttribute("appliedCoupon");
        double totalPrice = cartService.calculateTotal(session);
        double discountAmount = 0;

        if (appliedCouponCode != null) {
            Coupon coupon = couponService.findByCode(appliedCouponCode).orElse(null);
            if (coupon != null) {
                if (Boolean.TRUE.equals(coupon.getActive())) {
                    if (totalPrice >= coupon.getMinOrderValue().doubleValue()) {
                        if ("PERCENT".equalsIgnoreCase(coupon.getDiscountType())
                                || "PERCENTAGE".equalsIgnoreCase(coupon.getDiscountType())) {
                            discountAmount = totalPrice * (coupon.getDiscountValue().doubleValue() / 100.0);
                        } else {
                            discountAmount = coupon.getDiscountValue().doubleValue();
                        }
                    } else {
                        model.addAttribute("couponError", "Đơn hàng chưa đạt giá trị tối thiểu " + coupon.getMinOrderValue() + "đ để áp dụng mã này.");
                        session.removeAttribute("appliedCoupon");
                        appliedCouponCode = null;
                    }
                } else {
                    model.addAttribute("couponError", "Mã giảm giá này hiện không còn hoạt động.");
                    session.removeAttribute("appliedCoupon");
                    appliedCouponCode = null;
                }
            } else {
                model.addAttribute("couponError", "Mã giảm giá không tồn tại.");
                session.removeAttribute("appliedCoupon");
                appliedCouponCode = null;
            }
        }

        model.addAttribute("cartItems", cart.values());
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("discountAmount", discountAmount);
        model.addAttribute("addresses", addresses);
        model.addAttribute("appliedCoupon", appliedCouponCode);

        boolean canPurchase = userService.canUserAddToCart(user.getId());
        model.addAttribute("canAddToCart", canPurchase);

        // If there's a success param from redirect
        if (session.getAttribute("orderSuccess") != null) {
            model.addAttribute("success", "Đặt hàng thành công!");
            session.removeAttribute("orderSuccess");
        }

        return "checkout/checkout";
    }

    @PostMapping("/apply-coupon")
    public String applyCoupon(@RequestParam String code, HttpSession session) {
        if (code == null || code.trim().isEmpty()) {
            session.removeAttribute("appliedCoupon");
        } else {
            session.setAttribute("appliedCoupon", code.trim().toUpperCase());
        }
        return "redirect:/checkout";
    }

    @PostMapping("/place-order")
    public String placeOrder(HttpSession session,
            Principal principal,
            @RequestParam String address,
            @RequestParam String paymentMethod,
            @RequestParam String recipientName,
            @RequestParam String recipientPhone,
            @RequestParam(required = false) String notes,
            Model model) {

        if (principal == null) {
            return "redirect:/login";
        }

        User user = userService.findByUsername(principal.getName());
        if (!userService.canUserAddToCart(user.getId())) {
            model.addAttribute("error", "Quyền mua hàng của bạn đã bị thu hồi. Liên hệ admin.");
            model.addAttribute("cartItems", cartService.getCart(session).values());
            model.addAttribute("totalPrice", cartService.calculateTotal(session));
            model.addAttribute("addresses", userAddressService.findByUser(user));
            return "cart/cart";
        }

        String couponCode = (String) session.getAttribute("appliedCoupon");

        List<CartItemDTO> cartItems = new ArrayList<>(cartService.getCart(session).values());
        if (cartItems.isEmpty()) {
            return "redirect:/cart";
        }
        try {
            String redirectUrl = orderService.placeOrder(cartItems, couponCode, address, paymentMethod, notes, recipientName, recipientPhone, user);

            // Clear cart and coupon upon successful order placement
            cartService.clearCart(session);
            session.removeAttribute("appliedCoupon");

            return "redirect:" + redirectUrl;
        } catch (RuntimeException e) {
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("cartItems", cartItems);
            model.addAttribute("totalPrice", cartService.calculateTotal(session));
            model.addAttribute("addresses", userAddressService.findByUser(user));
            model.addAttribute("appliedCoupon", couponCode);
            return "checkout/checkout";
        }
    }

    @GetMapping("/callback")
    public String paymentCallback(
            Model model,
            @RequestParam("orderCode") String orderCode,
            @RequestParam("status") String status,
            @RequestParam("cancel") boolean cancel,
            HttpSession session) {

        Long parsedOrderCode = Long.parseLong(orderCode);
        Order order = orderService.processOrder(parsedOrderCode, status, cancel, session);
        if (order == null) {
            model.addAttribute("error", "Không tìm thấy đơn hàng hoặc đã xử lý trước đó.");
            return "payment/callback";
        }
        model.addAttribute("order", order);
        model.addAttribute("status", status);
        return "payment/callback";
    }

    @GetMapping("/checkout")
    public String orderCallback(
            Model model,
            @RequestParam("orderCode") String orderCode,
            @RequestParam("success") boolean success,
            HttpSession session) {
        Order order = orderService.orderCallback(Long.parseLong(orderCode), success, session);
        model.addAttribute("order", order);
        return "payment/order-callback";
    }

}

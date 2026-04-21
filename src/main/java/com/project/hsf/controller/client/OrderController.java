package com.project.hsf.controller.client;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.project.hsf.entity.Order;
import com.project.hsf.entity.OrderStatus;
import com.project.hsf.entity.OrderStatusHistory;
import com.project.hsf.entity.OrderItem;
import com.project.hsf.entity.PaymentStatus;
import com.project.hsf.entity.User;
import com.project.hsf.service.OrderService;
import com.project.hsf.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final UserService userService;
    private final OrderService orderService;

    @GetMapping
    public String viewOrders(
            Authentication authentication,
            Model model,
            @RequestParam(required = false) OrderStatus orderStatus,
            @RequestParam(required = false) PaymentStatus paymentStatus) {
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        List<Order> orders = orderService.getOrdersByUserWithFilters(user, orderStatus, paymentStatus);
        model.addAttribute("user", user);
        model.addAttribute("orders", orders);
        model.addAttribute("orderStatus", orderStatus);
        model.addAttribute("paymentStatus", paymentStatus);
        model.addAttribute("orderStatuses", OrderStatus.values());
        model.addAttribute("paymentStatuses", PaymentStatus.values());
        return "order/orders";
    }

    @GetMapping("/{id}")
    public String viewOrderDetail(
            @PathVariable Long id,
            Authentication authentication,
            Model model) {
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        Order order = orderService.getOrderById(id, user);
        if (order == null) {
            return "redirect:/orders?error=notfound";
        }
        List<OrderItem> orderItems = orderService.getOrderItems(id);
        List<OrderStatusHistory> statusHistories = orderService.getOrderStatusHistory(id);
        model.addAttribute("user", user);
        model.addAttribute("order", order);
        model.addAttribute("orderItems", orderItems);
        model.addAttribute("statusHistories", statusHistories);
        return "order/order-detail";
    }
}

package com.project.hsf.service;

import java.util.List;

import com.project.hsf.dto.CartItemDTO;
import com.project.hsf.entity.Order;
import com.project.hsf.entity.OrderItem;
import com.project.hsf.entity.OrderStatus;
import com.project.hsf.entity.OrderStatusHistory;
import com.project.hsf.entity.PaymentStatus;
import com.project.hsf.entity.User;

public interface OrderService {
    List<Order> getOrdersByUser(User user);

    List<Order> getOrdersByUserWithFilters(User user, OrderStatus orderStatus, PaymentStatus paymentStatus);

    Order getOrderById(Long id, User user);

    Order getOrderById(Long id);

    List<Order> getAllOrders();

    List<Order> getAllOrders(org.springframework.data.domain.Sort sort);

    void updateOrderStatus(Long orderId, OrderStatus status, String note);

    List<OrderItem> getOrderItems(Long orderId);

    List<OrderStatusHistory> getOrderStatusHistory(Long orderId);

    List<Order> getOrdersByCustomer(User customer);

    String placeOrder(List<CartItemDTO> cartItems, String couponCode, String shippingAddress, String paymentMethod, String notes, User customer) throws RuntimeException;

    Order processOrder(Long orderCode, String status, boolean cancel);

    Order orderCallback(Long orderCode, boolean success);
}

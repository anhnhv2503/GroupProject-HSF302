package com.project.hsf.service;

import java.util.List;
import java.util.Map;

import com.project.hsf.dto.CartItemDTO;
import com.project.hsf.entity.Order;
import com.project.hsf.entity.OrderItem;
import com.project.hsf.enums.OrderStatus;
import com.project.hsf.entity.OrderStatusHistory;
import com.project.hsf.enums.PaymentStatus;
import com.project.hsf.entity.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Sort;

public interface OrderService {
    List<Order> getOrdersByUser(User user);

    List<Order> getOrdersByUserWithFilters(User user, OrderStatus orderStatus, PaymentStatus paymentStatus, String orderCode);

    Order getOrderById(Long id, User user);

    Order getOrderById(Long id);

    List<Order> getAllOrders();

    List<Order> getAllOrders(Sort sort, String orderCode, String paymentMethod);

    void updateOrderStatus(Long orderId, OrderStatus status, String note);

    List<OrderItem> getOrderItems(Long orderId);

    List<OrderStatusHistory> getOrderStatusHistory(Long orderId);

    List<Order> getOrdersByCustomer(User customer);

    String placeOrder(List<CartItemDTO> cartItems, String couponCode, String shippingAddress, String paymentMethod, String notes, String recipientName, String recipientPhone, User customer) throws RuntimeException;

    Order processOrder(Long orderCode, String status, boolean cancel, HttpSession session);

    Order orderCallback(Long orderCode, boolean success, HttpSession session);

    List<Order> getNewestOrders();

    Map<String, Object> getOrderStatistics();
}

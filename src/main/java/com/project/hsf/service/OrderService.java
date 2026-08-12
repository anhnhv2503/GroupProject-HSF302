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

    /**
     * Handles the customer returning to the site from the PayOS payment page.
     *
     * Takes no status parameter from the URL: nothing the browser says is trusted. If the customer
     * cancelled, the order is cancelled (after an ownership check); otherwise PayOS is asked directly
     * whether the money arrived. Returns null if the order does not exist or is not this customer's.
     */
    Order handlePaymentReturn(Long orderCode, boolean userCancelled, User customer, HttpSession session);

    /**
     * Reads an order by orderCode for the thank-you page, only if it belongs to the logged-in customer.
     */
    Order findOwnedOrderByCode(Long orderCode, User customer);

    /**
     * Cancels an order and compensates what was deducted at placement: stock and coupon usage.
     *
     * Returns true only if this call actually cancelled the order. Later calls return false, so
     * stock is never returned twice for the same order.
     */
    boolean cancelOrder(Long orderId, String reason, String changedBy);

    /**
     * Cancels bank-transfer orders past their payment window and returns their stock.
     * Returns the number of orders cancelled.
     */
    int expireUnpaidOrders();

    List<Order> getNewestOrders();

    Map<String, Object> getOrderStatistics();
}

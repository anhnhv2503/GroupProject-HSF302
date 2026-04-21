package com.project.hsf.service;

import com.project.hsf.dto.CartItemDTO;
import com.project.hsf.entity.Order;
import com.project.hsf.entity.OrderStatus;
import com.project.hsf.entity.PaymentStatus;
import com.project.hsf.entity.User;

import java.util.List;

public interface OrderService {
    List<Order> getOrdersByUser(User user);

    List<Order> getOrdersByUserWithFilters(User user, OrderStatus orderStatus, PaymentStatus paymentStatus);

    Order getOrderById(Long id, User user);

    Order placeOrder(List<CartItemDTO> items, String couponCode, String notes) throws RuntimeException;
}
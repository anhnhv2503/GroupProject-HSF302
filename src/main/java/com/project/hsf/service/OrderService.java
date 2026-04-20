package com.project.hsf.service;

import com.project.hsf.dto.CartItemDTO;
import com.project.hsf.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.hsf.entity.User;
import java.util.List;

@Service
public interface OrderService {
    void placeOrder(List<CartItemDTO> cartItems, String couponCode, String shippingAddress, String paymentMethod, String notes, User customer) throws RuntimeException;
}

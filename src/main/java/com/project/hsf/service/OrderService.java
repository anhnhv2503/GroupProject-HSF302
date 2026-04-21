package com.project.hsf.service;

import com.project.hsf.dto.CartItemDTO;
import com.project.hsf.entity.Order;
import com.project.hsf.entity.User;
import com.project.hsf.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public interface OrderService {
   public void placeOrder(List<CartItemDTO> cartItems, String couponCode, Object orderTmp) throws RuntimeException;
   public List<Order> getOrdersByCustomer(User customer);
}

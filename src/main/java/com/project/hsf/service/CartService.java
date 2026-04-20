package com.project.hsf.service;

import com.project.hsf.dto.CartItemDTO;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public interface CartService {

    @SuppressWarnings("unchecked")
    Map<String, CartItemDTO> getCart(HttpSession session);

    public void addToCart(HttpSession session, CartItemDTO item);
   public void updateQuantity(HttpSession session, String itemKey, int quantity);
   public void removeItem(HttpSession session, String itemKey);
   public void clearCart(HttpSession session);
   public Double calculateTotal(HttpSession session);
}

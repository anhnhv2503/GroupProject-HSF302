package com.project.hsf.service.impl;

import com.project.hsf.dto.CartItemDTO;
import com.project.hsf.service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class CartServiceImpl implements CartService {

    private static final String CART_SESSION_KEY = "CART";

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, CartItemDTO> getCart(HttpSession session) {
        Map<String, CartItemDTO> cart = (Map<String, CartItemDTO>) session.getAttribute(CART_SESSION_KEY);
        if (cart == null) {
            cart = new HashMap<>();
            session.setAttribute(CART_SESSION_KEY, cart);
        }
        return cart;
    }

    @Override
    public void addToCart(HttpSession session, CartItemDTO item) {
        Map<String, CartItemDTO> cart = getCart(session);
        String key = item.getItemKey();

        if (cart.containsKey(key)) {
            CartItemDTO existingItem = cart.get(key);
            existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity());
        } else {
            cart.put(key, item);
        }
        session.setAttribute(CART_SESSION_KEY, cart);
    }


    @Override
    public void updateQuantity(HttpSession session, String itemKey, int quantity) {
        Map<String, CartItemDTO> cart = getCart(session);
        if (cart.containsKey(itemKey)) {
            if (quantity <= 0) {
                cart.remove(itemKey);
            } else {
                cart.get(itemKey).setQuantity(quantity);
            }
        }
        session.setAttribute(CART_SESSION_KEY, cart);
    }


    @Override
    public void removeItem(HttpSession session, String itemKey) {
        Map<String, CartItemDTO> cart = getCart(session);
        cart.remove(itemKey);
        session.setAttribute(CART_SESSION_KEY, cart);
    }


    @Override
    public void clearCart(HttpSession session) {
        session.removeAttribute(CART_SESSION_KEY);
    }


    @Override
    public Double calculateTotal(HttpSession session) {
        return getCart(session).values().stream()
                .mapToDouble(CartItemDTO::getSubtotal)
                .sum();
    }
}

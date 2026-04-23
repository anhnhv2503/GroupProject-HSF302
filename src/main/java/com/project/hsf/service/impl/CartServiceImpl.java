package com.project.hsf.service.impl;

import com.project.hsf.dto.CartItemDTO;
import com.project.hsf.entity.SeafoodProduct;
import com.project.hsf.service.CartService;
import com.project.hsf.service.SeafoodProductService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final SeafoodProductService productService;
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
        SeafoodProduct product = productService.findById(Long.valueOf(item.getProductId()));
        if (product == null)
            throw new RuntimeException("Sản phẩm không tồn tại");

        Map<String, CartItemDTO> cart = getCart(session);
        String key = item.getItemKey();

        int currentInCart = cart.containsKey(key) ? cart.get(key).getQuantity() : 0;
        int requestedTotal = currentInCart + item.getQuantity();

        if (product.getStockQuantity() < requestedTotal) {
            throw new RuntimeException(
                    "Sản phẩm " + product.getName() + " chỉ còn " + product.getStockQuantity() + " sản phẩm");
        }

        if (cart.containsKey(key)) {
            CartItemDTO existingItem = cart.get(key);
            existingItem.setQuantity(requestedTotal);
        } else {
            cart.put(key, item);
        }
        session.setAttribute(CART_SESSION_KEY, cart);
        updateCartCount(session, cart);
    }

    @Override
    public void updateQuantity(HttpSession session, String itemKey, int quantity) {
        Map<String, CartItemDTO> cart = getCart(session);
        if (cart.containsKey(itemKey)) {
            if (quantity <= 0) {
                cart.remove(itemKey);
            } else {
                CartItemDTO item = cart.get(itemKey);
                SeafoodProduct product = productService.findById(Long.valueOf(item.getProductId()));
                if (product != null && product.getStockQuantity() < quantity) {
                    throw new RuntimeException(
                            "Sản phẩm " + product.getName() + " chỉ còn " + product.getStockQuantity() + " sản phẩm");
                }
                item.setQuantity(quantity);
            }
        }
        session.setAttribute(CART_SESSION_KEY, cart);
        updateCartCount(session, cart);
    }

    @Override
    public void removeItem(HttpSession session, String itemKey) {
        Map<String, CartItemDTO> cart = getCart(session);
        cart.remove(itemKey);
        session.setAttribute(CART_SESSION_KEY, cart);
        updateCartCount(session, cart);
    }

    @Override
    public void clearCart(HttpSession session) {
        session.removeAttribute(CART_SESSION_KEY);
        session.setAttribute("cartCount", 0);
    }

    @Override
    public Double calculateTotal(HttpSession session) {
        return getCart(session).values().stream()
                .mapToDouble(CartItemDTO::getSubtotal)
                .sum();
    }

    private void updateCartCount(HttpSession session, Map<String, CartItemDTO> cart) {
        int count = cart.values().stream().mapToInt(CartItemDTO::getQuantity).sum();
        session.setAttribute("cartCount", count);
    }
}

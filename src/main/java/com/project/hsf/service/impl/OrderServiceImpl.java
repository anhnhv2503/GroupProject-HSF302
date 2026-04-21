package com.project.hsf.service.impl;

import com.project.hsf.dto.CartItemDTO;
import com.project.hsf.entity.Coupon;
import com.project.hsf.entity.Order;
import com.project.hsf.entity.OrderStatus;
import com.project.hsf.entity.PaymentStatus;
import com.project.hsf.entity.User;
import com.project.hsf.repository.CouponRepository;
import com.project.hsf.repository.OrderRepository;
import com.project.hsf.repository.UserRepository;
import com.project.hsf.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CouponRepository couponRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrdersByUser(User user) {
        return orderRepository.findByCustomerIdOrderByCreatedDateDesc(user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrdersByUserWithFilters(User user, OrderStatus orderStatus, PaymentStatus paymentStatus) {
        if (orderStatus == null && paymentStatus == null) {
            return getOrdersByUser(user);
        }
        return orderRepository.findByCustomerIdAndFilters(user.getId(), orderStatus, paymentStatus);
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrderById(Long id, User user) {
        return orderRepository.findByIdAndCustomerId(id, user.getId()).orElse(null);
    }

    @Override
    @Transactional
    public Order placeOrder(List<CartItemDTO> items, String couponCode, String notes) throws RuntimeException {
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (CartItemDTO item : items) {
            BigDecimal unitPrice = BigDecimal.valueOf(item.getUnitPrice());
            totalPrice = totalPrice.add(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        Coupon coupon = null;
        if (couponCode != null && !couponCode.isEmpty()) {
            coupon = couponRepository.findByCode(couponCode);
            if (coupon != null && coupon.getDiscountValue() != null) {
                if ("PERCENT".equals(coupon.getDiscountType())) {
                    discountAmount = totalPrice.multiply(coupon.getDiscountValue()).divide(BigDecimal.valueOf(100));
                } else {
                    discountAmount = coupon.getDiscountValue();
                }
            }
        }

        BigDecimal shippingFee = BigDecimal.ZERO;
        if (totalPrice.compareTo(new BigDecimal("5000000")) < 0) {
            shippingFee = new BigDecimal("30000");
        }

        BigDecimal finalPrice = totalPrice.subtract(discountAmount).add(shippingFee);

        User user = userRepository.findByUsername("customer");
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Order order = Order.builder()
                .customer(user)
                .totalPrice(totalPrice)
                .discountAmount(discountAmount)
                .shippingFee(shippingFee)
                .finalPrice(finalPrice)
                .orderStatus(OrderStatus.PENDING)
                .paymentMethod("COD")
                .paymentStatus(PaymentStatus.UNPAID)
                .shippingAddress("Test Address")
                .couponCode(coupon)
                .notes(notes)
                .build();

        return orderRepository.save(order);
    }
}
package com.project.hsf.service.impl;

import com.project.hsf.dto.CartItemDTO;
import com.project.hsf.repository.CouponRepository;
import com.project.hsf.repository.SeafoodProductRepository;
import com.project.hsf.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serial;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {


    private final SeafoodProductRepository seafoodProductRepository;
    private final CouponRepository couponRepository;
    // Assuming these repositories exist
    // private final OrderRepository orderRepository;
    // private final OrderItemRepository orderItemRepository;
    // private final PaymentRepository paymentRepository;
    // private final CouponUsageRepository couponUsageRepository;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void placeOrder(List<CartItemDTO> cartItems, String couponCode, /*Order order, Payment payment*/ Object orderTmp) throws RuntimeException {
        // Atomic Stock Deduct
        for (CartItemDTO item : cartItems) {
            if (item.getProductId() != null) {
                int affected = seafoodProductRepository.deductStock(Long.valueOf(item.getProductId()), item.getQuantity());
                if (affected == 0) {
                    throw new RuntimeException("Hết hàng hoặc sản phẩm không khả dụng: " + item.getName());
                }
            }
            // Add combo deductive logic if needed
        }

        // Atomic Coupon Claim
        if (couponCode != null && !couponCode.isEmpty()) {
            int affected = couponRepository.claimCoupon(couponCode);
            if (affected == 0) {
                throw new RuntimeException("Coupon không hợp lệ, đã hết hạn hoặc hết lượt sử dụng.");
            }
            // insert coupon usage
            // couponUsageRepository.save(new CouponUsage(...));
        }

        // Insert Order
        // orderRepository.save(order);

        // Insert OrderItems
        // for (CartItemDTO item : cartItems) {
        //     orderItemRepository.save(new OrderItem(...));
        // }

        // Insert Payment
        // paymentRepository.save(payment);
    }
}

package com.project.hsf.service.impl;

import com.project.hsf.dto.CartItemDTO;
import com.project.hsf.entity.*;
import com.project.hsf.repository.*;
import com.project.hsf.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {


    private final SeafoodProductRepository seafoodProductRepository;
    private final CouponRepository couponRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final CouponUsageRepository couponUsageRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void placeOrder(List<CartItemDTO> cartItems, String couponCode, String shippingAddress, String paymentMethod, String notes, User customer) throws RuntimeException {
        
        BigDecimal subtotal = BigDecimal.ZERO;
        
        // 1. Stock Deduction & Subtotal Calculation
        for (CartItemDTO item : cartItems) {
            if (item.getProductId() != null) {
                int affected = seafoodProductRepository.deductStock(Long.valueOf(item.getProductId()), item.getQuantity());
                if (affected == 0) {
                    throw new RuntimeException("Sản phẩm " + item.getName() + " hiện đã hết hàng hoặc không đủ số lượng.");
                }
            }
            subtotal = subtotal.add(BigDecimal.valueOf(item.getUnitPrice()).multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        // 2. Coupon Validation & Calculation
        BigDecimal discountAmount = BigDecimal.ZERO;
        Coupon usedCoupon = null;
        if (couponCode != null && !couponCode.isEmpty()) {
            usedCoupon = couponRepository.findByCode(couponCode)
                    .orElseThrow(() -> new RuntimeException("Mã giảm giá không hợp lệ."));
            
            if (subtotal.compareTo(usedCoupon.getMinOrderValue()) < 0) {
                throw new RuntimeException("Giá trị đơn hàng tối thiểu để áp dụng mã này là " + usedCoupon.getMinOrderValue() + "đ");
            }

            int claimed = couponRepository.claimCoupon(couponCode);
            if (claimed == 0) {
                throw new RuntimeException("Mã giảm giá đã hết lượt sử dụng.");
            }

            if ("PERCENT".equals(usedCoupon.getDiscountType())) {
                discountAmount = subtotal.multiply(usedCoupon.getDiscountValue()).divide(BigDecimal.valueOf(100));
            } else {
                discountAmount = usedCoupon.getDiscountValue();
            }
        }

        BigDecimal shippingFee = BigDecimal.ZERO; // Free shipping for now
        BigDecimal finalPrice = subtotal.subtract(discountAmount).add(shippingFee);

        // 3. Save Order
        Order order = Order.builder()
                .customer(customer)
                .totalPrice(subtotal)
                .discountAmount(discountAmount)
                .shippingFee(shippingFee)
                .finalPrice(finalPrice)
                .orderStatus("PENDING")
                .paymentMethod(paymentMethod)
                .paymentStatus("UNPAID")
                .shippingAddress(shippingAddress)
                .couponCode(usedCoupon)
                .notes(notes)
                .createdDate(Instant.now())
                .updatedDate(Instant.now())
                .build();
        
        Order savedOrder = orderRepository.save(order);

        // 4. Save Order Items
        for (CartItemDTO itemDTO : cartItems) {
            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .productName(itemDTO.getName())
                    .productImageUrl(itemDTO.getImageUrl())
                    .quantity(itemDTO.getQuantity())
                    .unitPrice(BigDecimal.valueOf(itemDTO.getUnitPrice()))
                    .subtotal(BigDecimal.valueOf(itemDTO.getUnitPrice()).multiply(BigDecimal.valueOf(itemDTO.getQuantity())))
                    .createdDate(Instant.now())
                    .build();
            
            if (itemDTO.getProductId() != null) {
                SeafoodProduct product = seafoodProductRepository.findById(Long.valueOf(itemDTO.getProductId())).orElse(null);
                orderItem.setProduct(product);
            }
            
            orderItemRepository.save(orderItem);
        }

        // 5. Save Payment Record
        Payment payment = Payment.builder()
                .order(savedOrder)
                .paymentMethod(paymentMethod)
                .amount(finalPrice)
                .status("PENDING")
                .createdDate(Instant.now())
                .updatedDate(Instant.now())
                .build();
        paymentRepository.save(payment);

//        // 6. Save Coupon Usage
//        if (usedCoupon != null) {
//            CouponUsage usage = CouponUsage.builder()
//                    .user(customer)
//                    .coupon(usedCoupon)
//                    .usedAt(Instant.now())
//                    .build();
//            couponUsageRepository.save(usage);
//        }
    }
    }

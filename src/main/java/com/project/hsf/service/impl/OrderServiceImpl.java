package com.project.hsf.service.impl;

import com.project.hsf.dto.CartItemDTO;
import com.project.hsf.entity.*;
import com.project.hsf.enums.OrderStatus;
import com.project.hsf.enums.PaymentMethod;
import com.project.hsf.enums.PaymentStatus;
import com.project.hsf.repository.*;
import com.project.hsf.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {


    private final SeafoodProductRepository seafoodProductRepository;
    private final CouponRepository couponRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final PayOS payOS;

    private final String CALLBACK_URL = "http://localhost:8080/checkout/callback";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String placeOrder(
            List<CartItemDTO> cartItems,
            String couponCode,
            String shippingAddress,
            String paymentMethod,
            String notes,
            User customer) throws RuntimeException {

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

            if ("PERCENT" .equals(usedCoupon.getDiscountType())) {
                discountAmount = subtotal.multiply(usedCoupon.getDiscountValue()).divide(BigDecimal.valueOf(100));
            } else {
                discountAmount = usedCoupon.getDiscountValue();
            }
        }

        BigDecimal shippingFee = BigDecimal.ZERO; // Free shipping for now
        BigDecimal finalPrice = subtotal.subtract(discountAmount).add(shippingFee);
        long orderCode = System.currentTimeMillis() / 1000;
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
                .orderCode(orderCode)
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

        //Method: BANK_TRANSFER / COD

        if (PaymentMethod.BANK_TRANSFER.name().equals(paymentMethod)) {
            PaymentLinkItem item = PaymentLinkItem.builder()
                    .name("Thanh toán đơn hàng")
                    .quantity(1)
                    .price(finalPrice.multiply(BigDecimal.valueOf(1)).longValue())
                    .build();

            CreatePaymentLinkRequest request = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .amount(finalPrice.multiply(BigDecimal.valueOf(1)).longValue())
                    .description("Thanh toán đơn hàng")
                    .item(item)
                    .returnUrl(CALLBACK_URL)
                    .cancelUrl(CALLBACK_URL)
                    .expiredAt((System.currentTimeMillis() / 1000) + (30 * 60))
                    .build();
            CreatePaymentLinkResponse response = payOS.paymentRequests().create(request);
            return response.getCheckoutUrl();
        }

//        http://localhost:8080/checkout/checkout?success=true
        return "checkout?success=true&orderCode=" + savedOrder.getOrderCode();
    }

    @Override
    public Order processOrder(Long orderCode, String status, boolean cancel) {
        Order order = orderRepository.findByOrderCode(orderCode).orElse(null);

        if (order != null) {
            Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
            if (cancel) {
                order.setOrderStatus(OrderStatus.CANCELLED.name());
                order.setPaymentStatus(PaymentStatus.CANCELLED.name());
                payment.setStatus(PaymentStatus.CANCELLED.name());
            } else if ("PAID".equals(status)) {
                order.setOrderStatus(OrderStatus.IN_PROGRESS.name());
                order.setPaymentStatus(PaymentStatus.PAID.name());
                payment.setStatus(PaymentStatus.PAID.name());
            } else {
                order.setOrderStatus(OrderStatus.CANCELLED.name());
                order.setPaymentStatus(PaymentStatus.CANCELLED.name());
                payment.setStatus(PaymentStatus.CANCELLED.name());
            }
            paymentRepository.save(payment);
            return orderRepository.save(order);
        }
        return null;
    }

    @Override
    public Order orderCallback(Long orderCode, boolean success) {
        Order order = orderRepository.findByOrderCode(orderCode).orElse(null);

        if (order != null) {
            Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
            if (success) {
                order.setOrderStatus(OrderStatus.PENDING.name());
                order.setPaymentStatus(PaymentStatus.PENDING.name());
                payment.setStatus(PaymentStatus.PENDING.name());
            } else {
                order.setOrderStatus(OrderStatus.CANCELLED.name());
                order.setPaymentStatus(PaymentStatus.CANCELLED.name());
                payment.setStatus(PaymentStatus.CANCELLED.name());
            }
            paymentRepository.save(payment);
            return orderRepository.save(order);
        }
        return null;
    }
}

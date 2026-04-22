package com.project.hsf.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.project.hsf.service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;

import com.project.hsf.dto.CartItemDTO;
import com.project.hsf.entity.Coupon;
import com.project.hsf.entity.Order;
import com.project.hsf.entity.OrderItem;
import com.project.hsf.enums.OrderStatus;
import com.project.hsf.entity.OrderStatusHistory;
import com.project.hsf.entity.Payment;
import com.project.hsf.enums.PaymentStatus;
import com.project.hsf.entity.SeafoodProduct;
import com.project.hsf.entity.User;
import com.project.hsf.enums.PaymentMethod;
import com.project.hsf.repository.CouponRepository;
import com.project.hsf.repository.OrderItemRepository;
import com.project.hsf.repository.OrderRepository;
import com.project.hsf.repository.OrderStatusHistoryRepository;
import com.project.hsf.repository.PaymentRepository;
import com.project.hsf.repository.SeafoodProductRepository;
import com.project.hsf.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final SeafoodProductRepository seafoodProductRepository;
    private final CouponRepository couponRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final PaymentRepository paymentRepository;
    private final PayOS payOS;
    private final CartService cartService;

    private final String CALLBACK_URL = "http://localhost:8080/checkout/callback";

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrdersByCustomer(User customer) {
        return orderRepository.findByCustomerOrderByCreatedDateDesc(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getAllOrders(org.springframework.data.domain.Sort sort) {
        return orderRepository.findAll(sort);
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus status, String note) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay don hang voi id: " + orderId));

        order.setOrderStatus(status);
        order.setUpdatedDate(Instant.now());
        orderRepository.save(order);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(status);
        history.setChangedBy("Admin");
        history.setChangedAt(Instant.now());
        history.setNote(note);
        orderStatusHistoryRepository.save(history);
    }

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
    @Transactional(readOnly = true)
    public List<OrderItem> getOrderItems(Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderStatusHistory> getOrderStatusHistory(Long orderId) {
        List<OrderStatusHistory> histories = orderStatusHistoryRepository.findByOrderIdOrderByChangedAtAsc(orderId);
        java.util.Collections.reverse(histories);
        return histories;
    }

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

            if ("PERCENT".equals(usedCoupon.getDiscountType())) {
                discountAmount = subtotal.multiply(usedCoupon.getDiscountValue()).divide(BigDecimal.valueOf(100));
            } else {
                discountAmount = usedCoupon.getDiscountValue();
            }
        }

        BigDecimal shippingFee = BigDecimal.ZERO;
        BigDecimal finalPrice = subtotal.subtract(discountAmount).add(shippingFee);
        long orderCode = System.currentTimeMillis() / 1000;

        // 3. Save Order
        Order order = Order.builder()
                .customer(customer)
                .totalPrice(subtotal)
                .discountAmount(discountAmount)
                .shippingFee(shippingFee)
                .finalPrice(finalPrice)
                .orderStatus(OrderStatus.PENDING)
                .paymentMethod(paymentMethod)
                .paymentStatus(PaymentStatus.UNPAID)
                .shippingAddress(shippingAddress)
                .couponCode(usedCoupon)
                .notes(notes)
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

        if (PaymentMethod.BANK_TRANSFER.name().equals(paymentMethod)) {
            PaymentLinkItem item = PaymentLinkItem.builder()
                    .name("Thanh toán đơn hàng")
                    .quantity(1)
                    .price(finalPrice.longValue())
                    .build();

            CreatePaymentLinkRequest request = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .amount(finalPrice.longValue())
                    .description("Thanh toán đơn hàng")
                    .item(item)
                    .returnUrl(CALLBACK_URL)
                    .cancelUrl(CALLBACK_URL)
                    .expiredAt((System.currentTimeMillis() / 1000) + (30 * 60))
                    .build();
            CreatePaymentLinkResponse response = payOS.paymentRequests().create(request);
            return response.getCheckoutUrl();
        }

        return "checkout?success=true&orderCode=" + savedOrder.getOrderCode();
    }

    @Override
    public Order processOrder(Long orderCode, String status, boolean cancel, HttpSession session) {
        Order order = orderRepository.findByOrderCode(orderCode).orElse(null);

        if (order != null) {
            Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
            if (cancel) {
                order.setOrderStatus(OrderStatus.CANCELLED);
                order.setPaymentStatus(PaymentStatus.UNPAID);
                if (payment != null) payment.setStatus("CANCELLED");
            } else if ("PAID".equals(status)) {
                order.setOrderStatus(OrderStatus.CONFIRMED);
                order.setPaymentStatus(PaymentStatus.PAID);
                if (payment != null) payment.setStatus("PAID");
            } else {
                order.setOrderStatus(OrderStatus.CANCELLED);
                order.setPaymentStatus(PaymentStatus.UNPAID);
                if (payment != null) payment.setStatus("CANCELLED");
            }
            if (payment != null) paymentRepository.save(payment);
            Order savedOrder = orderRepository.save(order);
            cartService.clearCart(session);
            return savedOrder;
        }
        return null;
    }

    @Override
    public Order orderCallback(Long orderCode, boolean success, HttpSession session) {
        Order order = orderRepository.findByOrderCode(orderCode).orElse(null);

        if (order != null) {
            Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
            if (success) {
                order.setOrderStatus(OrderStatus.PENDING);
                order.setPaymentStatus(PaymentStatus.UNPAID);
                if (payment != null) payment.setStatus("PENDING");
            } else {
                order.setOrderStatus(OrderStatus.CANCELLED);
                order.setPaymentStatus(PaymentStatus.UNPAID);
                if (payment != null) payment.setStatus("CANCELLED");
            }
            if (payment != null) paymentRepository.save(payment);
            Order savedOrder = orderRepository.save(order);
            cartService.clearCart(session);
            return savedOrder;
        }

        return null;
    }
}

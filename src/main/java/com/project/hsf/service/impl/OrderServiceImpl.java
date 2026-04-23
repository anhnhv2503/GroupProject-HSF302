package com.project.hsf.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public List<Order> getAllOrders(org.springframework.data.domain.Sort sort, String orderCode, String paymentMethod) {
        String code = (orderCode == null || orderCode.isBlank()) ? null : orderCode.trim();
        String method = (paymentMethod == null || paymentMethod.isBlank()) ? null : paymentMethod.trim();
        
        if (code == null && method == null) {
            return orderRepository.findAll(sort);
        }
        return orderRepository.findAllWithFilters(code, method);
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus targetStatus, String note) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay don hang voi id: " + orderId));

        OrderStatus currentStatus = order.getOrderStatus();

        // 1. Terminal State Check
        if (currentStatus == OrderStatus.DELIVERED || currentStatus == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Khong the thay doi trang thai cua don hang da ket thuc (" + currentStatus + ").");
        }

        // 2. State Transition Validation
        boolean isValidTransition = switch (currentStatus) {
            case PENDING -> targetStatus == OrderStatus.CONFIRMED || targetStatus == OrderStatus.CANCELLED;
            case CONFIRMED -> targetStatus == OrderStatus.PROCESSING || targetStatus == OrderStatus.CANCELLED;
            case PROCESSING -> targetStatus == OrderStatus.SHIPPED || targetStatus == OrderStatus.CANCELLED;
            case SHIPPED -> targetStatus == OrderStatus.DELIVERED || targetStatus == OrderStatus.CANCELLED;
            default -> false;
        };

        if (!isValidTransition) {
            throw new IllegalStateException("Khong the chuyen tu trang thai " + currentStatus + " sang " + targetStatus + ".");
        }

        // 3. Payment Enforcement for Online Orders
        if (PaymentMethod.BANK_TRANSFER.name().equals(order.getPaymentMethod()) && targetStatus == OrderStatus.CONFIRMED) {
            if (order.getPaymentStatus() != PaymentStatus.PAID) {
                throw new IllegalStateException("Don hang chuyen khoan chua thanh toan thanh cong. Khong the xac nhan.");
            }
        }

        // 4. Side Effects on DELIVERED
        if (targetStatus == OrderStatus.DELIVERED) {
            if (PaymentMethod.COD.name().equals(order.getPaymentMethod())) {
                order.setPaymentStatus(PaymentStatus.PAID);
                Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
                if (payment != null) {
                    payment.setStatus("SUCCESS");
                    paymentRepository.save(payment);
                }
            }
            
            // Update Sold Count for each product in the order
            if (order.getOrderItems() != null) {
                for (OrderItem item : order.getOrderItems()) {
                    if (item.getProduct() != null) {
                        SeafoodProduct product = item.getProduct();
                        int currentSold = product.getSoldCount() != null ? product.getSoldCount() : 0;
                        product.setSoldCount(currentSold + item.getQuantity());
                        seafoodProductRepository.save(product);
                    }
                }
            }
        }

        // 5. Update Order & History
        order.setOrderStatus(targetStatus);
        order.setUpdatedDate(Instant.now());
        orderRepository.save(order);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(targetStatus);
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
    public List<Order> getOrdersByUserWithFilters(User user, OrderStatus orderStatus, PaymentStatus paymentStatus, String orderCode) {
        if (orderStatus == null && paymentStatus == null && (orderCode == null || orderCode.isBlank())) {
            return getOrdersByUser(user);
        }
        return orderRepository.findByCustomerIdAndFilters(user.getId(), orderStatus, paymentStatus, (orderCode != null && !orderCode.isBlank() ? orderCode.trim() : null));
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
            String recipientName,
            String recipientPhone,
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

        if (discountAmount.compareTo(subtotal) > 0) {
            discountAmount = subtotal;
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
                .orderStatus(OrderStatus.PENDING)
                .paymentMethod(paymentMethod)
                .paymentStatus(PaymentStatus.UNPAID)
                .shippingAddress(shippingAddress)
                .couponCode(usedCoupon)
                .notes(notes)
                .recipientName(recipientName)
                .recipientPhone(recipientPhone)
                .orderCode(orderCode)
                .finalPrice(finalPrice)
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
                if (payment != null) payment.setStatus("FAILED");
            } else if ("PAID".equals(status)) {
                order.setOrderStatus(OrderStatus.CONFIRMED);
                order.setPaymentStatus(PaymentStatus.PAID);
                if (payment != null) payment.setStatus("SUCCESS");
            } else {
                order.setOrderStatus(OrderStatus.CANCELLED);
                order.setPaymentStatus(PaymentStatus.UNPAID);
                if (payment != null) payment.setStatus("FAILED");
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
                if (payment != null) payment.setStatus("FAILED");
            }
            if (payment != null) paymentRepository.save(payment);
            Order savedOrder = orderRepository.save(order);
            cartService.clearCart(session);
            return savedOrder;
        }

        return null;
    }

    @Override
    public List<Order> getNewestOrders() {
        return orderRepository.findTop4ByOrderByCreatedDateDesc();
    }

    @Override
    public Map<String, Object> getOrderStatistics() {
        Map<String, Object> response = new HashMap<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        if(paymentRepository.count() > 0){
//            totalRevenue = paymentRepository.sumAmountByStatus("PAID");
            List<Payment> paidPayment = paymentRepository.findByStatus("PAID");
            for (Payment payment : paidPayment) {
                totalRevenue = totalRevenue.add(payment.getAmount());
            }
        }
        long totalOrder = orderRepository.count();
        response.put("totalOrder", totalOrder);
        response.put("totalRevenue", totalRevenue);
        return response;
    }
}

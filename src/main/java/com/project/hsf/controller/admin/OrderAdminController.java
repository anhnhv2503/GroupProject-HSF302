package com.project.hsf.controller.admin;

import com.project.hsf.entity.Order;
import com.project.hsf.entity.OrderItem;
import com.project.hsf.entity.OrderStatus;
import com.project.hsf.entity.OrderStatusHistory;
import com.project.hsf.repository.OrderItemRepository;
import com.project.hsf.repository.OrderRepository;
import com.project.hsf.repository.OrderStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class OrderAdminController {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

    @GetMapping
    public String list(@RequestParam(required = false) Long orderId, Model model) {
        List<Order> orders = orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdDate"));

        Order selectedOrder = null;
        if (!orders.isEmpty()) {
            if (orderId != null) {
                selectedOrder = orders.stream().filter(o -> o.getId().equals(orderId)).findFirst().orElse(orders.get(0));
            } else {
                selectedOrder = orders.get(0);
            }
        }

        List<OrderItem> selectedItems = selectedOrder != null
                ? orderItemRepository.findByOrderIdOrderByIdAsc(selectedOrder.getId())
                : Collections.emptyList();

        List<OrderStatusHistory> statusHistories = selectedOrder != null
                ? orderStatusHistoryRepository.findByOrderIdOrderByChangedAtAsc(selectedOrder.getId())
                : Collections.emptyList();

        model.addAttribute("orders", orders);
        model.addAttribute("selectedOrder", selectedOrder);
        model.addAttribute("selectedItems", selectedItems);
        model.addAttribute("statusHistories", statusHistories);
        model.addAttribute("orderStatuses", OrderStatus.values());
        model.addAttribute("page", "orders");
        return "admin/order-manage";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam OrderStatus status,
                               @RequestParam(required = false) String note,
                               RedirectAttributes redirectAttributes) {
        try {
            Order order = orderRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Khong tim thay don hang voi id: " + id));

            order.setOrderStatus(status);
            order.setUpdatedDate(Instant.now());
            orderRepository.save(order);

            OrderStatusHistory history = new OrderStatusHistory();
            history.setOrder(order);
            history.setStatus(status);
            history.setChangedBy("Admin");
            history.setChangedAt(Instant.now());
            history.setNote((note == null || note.isBlank()) ? defaultNoteForStatus(status) : note.trim());
            orderStatusHistoryRepository.save(history);

            redirectAttributes.addFlashAttribute("successMessage", "Cap nhat trang thai don hang thanh cong!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cap nhat trang thai that bai: " + ex.getMessage());
        }
        return "redirect:/admin/orders?orderId=" + id;
    }

    private String defaultNoteForStatus(OrderStatus status) {
        return switch (status) {
            case CONFIRMED -> "Don hang da duoc xac nhan.";
            case PROCESSING -> "Don hang dang duoc xu ly.";
            case SHIPPING, SHIPPED -> "Don hang dang duoc giao.";
            case DELIVERED -> "Don hang da giao thanh cong.";
            case CANCELLED -> "Don hang da bi huy.";
            default -> "Trang thai don hang da duoc cap nhat.";
        };
    }
}

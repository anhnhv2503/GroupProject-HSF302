package com.project.hsf.controller.admin;

import com.project.hsf.entity.Order;
import com.project.hsf.entity.OrderItem;
import com.project.hsf.enums.OrderStatus;
import com.project.hsf.entity.OrderStatusHistory;
import com.project.hsf.service.OrderService;
import com.project.hsf.service.OrderItemService;
import com.project.hsf.service.OrderStatusHistoryService;

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

import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class OrderAdminController {

    private final OrderService orderService;
    private final OrderItemService orderItemService;
    private final OrderStatusHistoryService orderStatusHistoryService;

    @GetMapping
    public String list(@RequestParam(required = false) Long orderId, Model model) {
        List<Order> orders = orderService.getAllOrders(Sort.by(Sort.Direction.DESC, "createdDate"));

        Order selectedOrder = null;
        if (!orders.isEmpty()) {
            if (orderId != null) {
                selectedOrder = orders.stream().filter(o -> o.getId().equals(orderId)).findFirst().orElse(orders.get(0));
            } else {
                selectedOrder = orders.get(0);
            }
        }

        List<OrderItem> selectedItems = selectedOrder != null
                ? orderItemService.findByOrderIdOrderByIdAsc(selectedOrder.getId())
                : Collections.emptyList();

        List<OrderStatusHistory> statusHistories = selectedOrder != null
                ? orderStatusHistoryService.findByOrderIdOrderByChangedAtAsc(selectedOrder.getId())
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
            String finalNote = (note == null || note.isBlank()) ? defaultNoteForStatus(status) : note.trim();
            orderService.updateOrderStatus(id, status, finalNote);
            
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

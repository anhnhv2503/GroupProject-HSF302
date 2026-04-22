package com.project.hsf.controller.admin;

import com.project.hsf.service.CategoryService;
import com.project.hsf.service.SeafoodProductService;
import com.project.hsf.service.ProductReviewService;
import com.project.hsf.entity.SeafoodProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final SeafoodProductService productService;
    private final CategoryService categoryService;
    private final ProductReviewService reviewService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("pendingCount", reviewService.getPendingReviewCount());
        // Basic stats derived from existing services
        model.addAttribute("totalProducts", productService.findAll().size());
        model.addAttribute("totalCategories", categoryService.findAll().size());
        
        // Dummy data for metrics expected by the UI
        int todayOrders = 12;
        long monthlyRevenue = 125000000L;
        List<SeafoodProduct> lowStockProducts = productService.search(null, null, true, true, "id", "desc");

        model.addAttribute("todayOrders", todayOrders);
        model.addAttribute("monthlyRevenue", monthlyRevenue);
        model.addAttribute("avgOrderValue", todayOrders > 0 ? monthlyRevenue / todayOrders : 0);
        model.addAttribute("lowStockProducts", lowStockProducts);
        model.addAttribute("lowStockCount", lowStockProducts.size());
        model.addAttribute("expiringProducts", 3);
        
        // Recent orders (Dummy)
        // In a real app, we would fetch these from OrderService
        model.addAttribute("recentOrders", List.of(
                Map.of("code", "ORD-2026-001", "customer", "Nguyen Van A", "date", "4/12/2026", "amount", 189.97, "status", "SHIPPING"),
                Map.of("code", "ORD-2026-002", "customer", "Tran Thi Binh", "date", "4/13/2026", "amount", 198.97, "status", "CONFIRMED"),
                Map.of("code", "ORD-2026-003", "customer", "Le Minh Chau", "date", "4/10/2026", "amount", 104.00, "status", "DELIVERED"),
                Map.of("code", "ORD-2026-004", "customer", "Pham Quoc Dung", "date", "4/14/2026", "amount", 188.48, "status", "PENDING")
        ));
        model.addAttribute("page", "dashboard");
        
        return "admin/dashboard";
    }

}

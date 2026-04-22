package com.project.hsf.controller.admin;

import com.project.hsf.entity.SeafoodProduct;
import com.project.hsf.service.CategoryService;
import com.project.hsf.service.OrderService;
import com.project.hsf.service.SeafoodProductService;
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
    private final OrderService orderService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Basic stats derived from existing services
        model.addAttribute("totalProducts", productService.findAll().size());
        model.addAttribute("totalCategories", categoryService.findAll().size());

        Map<String, Object> response = orderService.getOrderStatistics();

        // Dummy data for metrics expected by the UI
        List<SeafoodProduct> lowStockProducts = productService.search(null, null, true, true, "id", "desc");

        model.addAttribute("totalOrder", response.get("totalOrder"));
        model.addAttribute("totalRevenue", response.get("totalRevenue"));
//        model.addAttribute("avgOrderValue", todayOrders > 0 ? monthlyRevenue / todayOrders : 0);
        model.addAttribute("lowStockProducts", lowStockProducts);
        model.addAttribute("lowStockCount", lowStockProducts.size());
        model.addAttribute("expiringProducts", 3);

        // Recent orders (Dummy)
        // In a real app, we would fetch these from OrderService
        model.addAttribute("recentOrders", orderService.getNewestOrders());
        model.addAttribute("page", "dashboard");

        return "admin/dashboard";
    }

}

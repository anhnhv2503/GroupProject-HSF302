package com.project.hsf.controller.admin;

import com.project.hsf.service.CategoryService;
import com.project.hsf.service.SeafoodProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final SeafoodProductService productService;
    private final CategoryService categoryService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Basic stats derived from existing services
        model.addAttribute("totalProducts", productService.findAll().size());
        model.addAttribute("totalCategories", categoryService.findAll().size());
        
        // Dummy data for metrics expected by the UI
        model.addAttribute("todayOrders", 12);
        model.addAttribute("monthlyRevenue", 125000000);
        model.addAttribute("lowStockProducts", productService.search(null, null, true, true, "id", "desc"));
        model.addAttribute("expiringProducts", 3);
        
        // Recent orders (Dummy)
        // In a real app, we would fetch these from OrderService
        
        return "admin/dashboard";
    }
}

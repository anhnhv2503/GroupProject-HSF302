package com.project.hsf.controller.admin;

import com.project.hsf.service.CategoryService;
import com.project.hsf.service.SeafoodProductService;
import com.project.hsf.entity.SeafoodProduct;
import com.project.hsf.repository.OrderRepository;
import com.project.hsf.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.data.domain.PageRequest;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final SeafoodProductService productService;
    private final CategoryService categoryService;
    private final OrderRepository orderRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Basic stats derived from existing services
        model.addAttribute("totalProducts", productService.findAll().size());
        model.addAttribute("totalCategories", categoryService.findAll().size());

        ZoneId zoneId = ZoneId.systemDefault();
        Instant startOfToday = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant();
        Instant startOfTomorrow = LocalDate.now(zoneId).plusDays(1).atStartOfDay(zoneId).toInstant();
        Instant startOfMonth = LocalDate.now(zoneId).withDayOfMonth(1).atStartOfDay(zoneId).toInstant();

        long todayOrders = orderRepository.countByCreatedDateBetween(startOfToday, startOfTomorrow);
        long paidMonthOrders = orderRepository.countByCreatedDateBetweenAndPaymentStatus(startOfMonth, startOfTomorrow, com.project.hsf.entity.PaymentStatus.PAID);
        BigDecimal monthlyRevenue = orderRepository.sumRevenueBetween(startOfMonth, startOfTomorrow);
        if (monthlyRevenue == null) {
            monthlyRevenue = BigDecimal.ZERO;
        }
        List<SeafoodProduct> lowStockProducts = productService.search(null, null, true, true, "id", "desc");

        List<Order> recentOrders = orderRepository.findRecentOrdersWithCustomer(PageRequest.of(0, 5));

        model.addAttribute("todayOrders", todayOrders);
        model.addAttribute("monthlyRevenue", monthlyRevenue);
        model.addAttribute(
                "avgOrderValue",
            paidMonthOrders > 0
                ? monthlyRevenue.divide(BigDecimal.valueOf(paidMonthOrders), 0, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO
        );
        model.addAttribute("lowStockProducts", lowStockProducts);
        model.addAttribute("lowStockCount", lowStockProducts.size());
        model.addAttribute("expiringProducts", 3);

        model.addAttribute("recentOrders", recentOrders);
        model.addAttribute("page", "dashboard");
        
        return "admin/dashboard";
    }

}

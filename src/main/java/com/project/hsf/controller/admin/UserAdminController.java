package com.project.hsf.controller.admin;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.project.hsf.entity.Order;
import com.project.hsf.entity.User;
import com.project.hsf.service.OrderService;
import com.project.hsf.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserService userService;
    private final OrderService orderService;

    @GetMapping
    public String list(@RequestParam(required = false) String keyword, Model model) {
        List<User> users = userService.getAllUsers(Sort.by(Sort.Direction.DESC, "createdDate"));

        if (StringUtils.hasText(keyword)) {
            String normalizedKeyword = normalize(keyword);
            users = users.stream()
                    .filter(u -> containsNormalized(u.getFullName(), normalizedKeyword)
                            || containsNormalized(u.getUsername(), normalizedKeyword)
                            || containsNormalized(u.getEmail(), normalizedKeyword)
                            || containsNormalized(u.getPhone(), normalizedKeyword))
                    .toList();
        }

        List<Order> orders = orderService.getAllOrders();

        Map<Long, Long> orderCountByUser = orders.stream()
                .filter(o -> o.getCustomer() != null && o.getCustomer().getId() != null)
                .collect(Collectors.groupingBy(o -> o.getCustomer().getId(), Collectors.counting()));

        Map<Long, BigDecimal> spendingByUser = orders.stream()
                .filter(o -> o.getCustomer() != null && o.getCustomer().getId() != null)
                .collect(Collectors.groupingBy(
                        o -> o.getCustomer().getId(),
                        Collectors.mapping(
                                o -> o.getFinalPrice() != null ? o.getFinalPrice() : (o.getTotalPrice() != null ? o.getTotalPrice() : BigDecimal.ZERO),
                                Collectors.reducing(BigDecimal.ZERO, Function.identity(), BigDecimal::add)
                        )
                ));

        long activeUsers = users.stream().filter(u -> Boolean.TRUE.equals(u.getEnabled())).count();
        long customerUsers = users.stream().filter(u -> "CUSTOMER".equalsIgnoreCase(u.getRole())).count();

        model.addAttribute("users", users);
        model.addAttribute("keyword", keyword);
        model.addAttribute("orderCountByUser", orderCountByUser);
        model.addAttribute("spendingByUser", spendingByUser);
        model.addAttribute("totalUsers", users.size());
        model.addAttribute("customerUsers", customerUsers);
        model.addAttribute("activeUsers", activeUsers);
        model.addAttribute("blockedUsers", users.size() - activeUsers);
        model.addAttribute("page", "users");

        return "admin/user-manage";
    }

    @PostMapping("/{id}/permissions")
    public String updatePermissions(@PathVariable Long id,
                                  @RequestParam(defaultValue = "false") boolean enabled,
                                  @RequestParam(defaultValue = "false") boolean canAddToCart,
                                  @RequestParam(defaultValue = "false") boolean canReview) {
        try {
            User user = userService.findById(id);
            if (user == null) {
                return "redirect:/admin/users";
            }

            userService.toggleUserStatus(id, enabled);
            userService.toggleCartPermission(id, canAddToCart);
            userService.toggleReviewPermission(id, !canReview);
        } catch (Exception e) {
            // just reload anyway
        }

        return "redirect:/admin/users";
    }

    private boolean containsNormalized(String value, String normalizedKeyword) {
        return value != null && normalize(value).contains(normalizedKeyword);
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}

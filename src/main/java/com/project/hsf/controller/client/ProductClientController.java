package com.project.hsf.controller.client;
/*
 * Copyright (c) 2026 vinhung. All rights reserved.
 */

import com.project.hsf.entity.Order;
import com.project.hsf.enums.OrderStatus;
import com.project.hsf.entity.ProductReview;
import com.project.hsf.entity.SeafoodProduct;
import com.project.hsf.entity.User;
import com.project.hsf.service.CategoryService;
import com.project.hsf.service.OrderService;
import com.project.hsf.service.ProductReviewService;
import com.project.hsf.service.SeafoodProductService;
import com.project.hsf.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ProductClientController {

    private final SeafoodProductService productService;
    private final CategoryService categoryService;
    private final ProductReviewService reviewService;
    private final UserService userService;
    private final OrderService orderService;

    @GetMapping("/products")
    public String list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            Model model) {
        model.addAttribute("products", productService.search(q, categoryId, true, null, "id", "desc"));
        model.addAttribute("categories", categoryService.findByActiveTrue());
        model.addAttribute("selectedCategory", categoryId);
        return "user/product-list";
    }

    @GetMapping("/products/{id}")
    public String detail(@PathVariable Long id, Model model, Authentication authentication) {
        SeafoodProduct product = productService.findById(id);
        if (product == null) {
            return "redirect:/";
        }

        List<ProductReview> reviews = reviewService.getVisibleReviewsByProduct(id);
        model.addAttribute("product", product);
        model.addAttribute("reviews", reviews);

        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            User user = userService.findByUsername(username);
            if (user != null) {
                boolean hasReviewed = reviewService.hasUserReviewedProduct(id, user.getId());
                boolean canReview = false;
                Boolean userReviewVisible = null;

                if (hasReviewed) {
                    ProductReview userReview = reviewService.getUserReviewForProduct(id, user.getId());
                    userReviewVisible = userReview != null && userReview.getIsVisible();
                } else {
                    canReview = reviewService.canUserReviewProduct(id, user.getId()) && !Boolean.TRUE.equals(user.getIsCommentBlocked());
                }

                model.addAttribute("hasReviewed", hasReviewed);
                model.addAttribute("canReview", canReview);
                model.addAttribute("userReviewVisible", userReviewVisible);
            }
        }

        return "user/product-detail";
    }

    @PostMapping("/products/{id}/review")
    public String submitReview(
            @PathVariable Long id,
            @RequestParam Short rating,
            @RequestParam String comment,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        if (rating == null || rating < 1 || rating > 5) {
            redirectAttributes.addFlashAttribute("reviewError", "Vui lòng chọn số sao (1-5)");
            return "redirect:/products/" + id;
        }

        if (comment == null || comment.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("reviewError", "Vui lòng nhập nội dung đánh giá");
            return "redirect:/products/" + id;
        }

        String username = authentication.getName();
        User user = userService.findByUsername(username);
        SeafoodProduct product = productService.findById(id);

        if (product == null || user == null) {
            return "redirect:/products/" + id + "#reviews";
        }

        if (Boolean.TRUE.equals(user.getIsCommentBlocked())) {
            redirectAttributes.addFlashAttribute("reviewError", "Tài khoản của bạn đã bị khóa tính năng đánh giá");
            return "redirect:/products/" + id + "#reviews";
        }

        if (reviewService.hasUserReviewedProduct(id, user.getId())) {
            redirectAttributes.addFlashAttribute("reviewError", "Bạn đã đánh giá sản phẩm này rồi");
            return "redirect:/products/" + id + "#reviews";
        }

        if (!reviewService.canUserReviewProduct(id, user.getId())) {
            redirectAttributes.addFlashAttribute("reviewError", "Bạn cần mua sản phẩm này và đơn hàng phải được giao thành công để đánh giá");
            return "redirect:/products/" + id + "#reviews";
        }

        Order order = findDeliveredOrderWithProduct(user, id);
        reviewService.createReview(product, user, order, rating, comment);

        redirectAttributes.addFlashAttribute("reviewSuccess", "Đánh giá của bạn đã được gửi! Chờ quản trị viên duyệt.");
        return "redirect:/products/" + id + "#reviews";
    }

    private Order findDeliveredOrderWithProduct(User user, Long productId) {
        List<Order> orders = orderService.getOrdersByUser(user);
        return orders.stream()
                .filter(o -> o.getOrderStatus() == OrderStatus.DELIVERED)
                .filter(o -> o.getOrderItems().stream()
                        .anyMatch(item -> item.getProduct() != null &&
                                item.getProduct().getId().equals(productId)))
                .findFirst()
                .orElse(null);
    }
}


package com.project.hsf.controller.client;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.project.hsf.entity.Order;
import com.project.hsf.entity.OrderStatus;
import com.project.hsf.entity.ProductReview;
import com.project.hsf.entity.SeafoodProduct;
import com.project.hsf.entity.User;
import com.project.hsf.service.OrderService;
import com.project.hsf.service.ProductReviewService;
import com.project.hsf.service.SeafoodProductService;
import com.project.hsf.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ProductReviewService reviewService;
    private final OrderService orderService;
    private final SeafoodProductService productService;
    private final UserService userService;

    @GetMapping("/product/{productId}")
    public String viewProductReview(
            @PathVariable Long productId,
            Authentication authentication,
            Model model) {
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        SeafoodProduct product = productService.findById(productId);

        if (product == null) {
            return "redirect:/orders?error=productnotfound";
        }

        boolean hasReviewed = reviewService.hasUserReviewedProduct(productId, user.getId());
        boolean canReview = false;

        if (!hasReviewed) {
            canReview = reviewService.canUserReviewProduct(productId, user.getId());
        }

        List<ProductReview> reviews = reviewService.getReviewsByProduct(productId);

        model.addAttribute("user", user);
        model.addAttribute("product", product);
        model.addAttribute("reviews", reviews);
        model.addAttribute("hasReviewed", hasReviewed);
        model.addAttribute("canReview", canReview);

        return "review/product-review";
    }

    @PostMapping("/product/{productId}")
    public String createReview(
            @PathVariable Long productId,
            @RequestParam Short rating,
            @RequestParam String comment,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        SeafoodProduct product = productService.findById(productId);

        if (product == null) {
            redirectAttributes.addFlashAttribute("error", "Sản phẩm không tồn tại");
            return "redirect:/reviews/product/" + productId;
        }

        boolean hasReviewed = reviewService.hasUserReviewedProduct(productId, user.getId());
        if (hasReviewed) {
            redirectAttributes.addFlashAttribute("error", "Bạn đã đánh giá sản phẩm này rồi");
            return "redirect:/reviews/product/" + productId;
        }

        boolean canReview = reviewService.canUserReviewProduct(productId, user.getId());
        if (!canReview) {
            redirectAttributes.addFlashAttribute("error", "Bạn cần mua sản phẩm này và đơn hàng phải được giao thành công để đánh giá");
            return "redirect:/reviews/product/" + productId;
        }

        Order order = findDeliveredOrderWithProduct(user, productId);

        reviewService.createReview(product, user, order, rating, comment);

        redirectAttributes.addFlashAttribute("success", "Đánh giá của bạn đã được gửi!");
        return "redirect:/reviews/product/" + productId;
    }

    private Order findDeliveredOrderWithProduct(User user, Long productId) {
        if (user == null) return null;
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
package com.project.hsf.controller.admin;

import com.project.hsf.entity.ProductReview;
import com.project.hsf.entity.ProductReviewCount;
import com.project.hsf.entity.SeafoodProduct;
import com.project.hsf.service.CategoryService;
import com.project.hsf.service.ProductReviewService;
import com.project.hsf.service.SeafoodProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/seafood-products")
@RequiredArgsConstructor
public class SeafoodProductController {

    private final SeafoodProductService seafoodProductService;
    private final CategoryService categoryService;
    private final ProductReviewService reviewService;

    @GetMapping
    public String list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean lowStock,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Model model) {

        List<SeafoodProduct> products = seafoodProductService.search(keyword, categoryId, active, lowStock, sortBy, sortDir);
        model.addAttribute("products", products);
        model.addAttribute("categories", categoryService.findAll());

        // Pass filter values back to UI to maintain state
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("active", active);
        model.addAttribute("lowStock", lowStock);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);

        // Calculate Global Statistics (using all products for business overview)
        List<SeafoodProduct> allProducts = seafoodProductService.findAll();
        long totalProducts = allProducts.size();
        long activeProductsCount = allProducts.stream().filter(SeafoodProduct::getActive).count();
        long lowStockProductsCount = allProducts.stream().filter(p -> p.getStockQuantity() != null && p.getStockQuantity() < 10).count();

        // Get pending review count per product
        List<ProductReviewCount> reviewCounts = reviewService.getPendingReviewCountByProduct();
        Map<Long, Long> pendingReviewCounts = new HashMap<>();
        for (ProductReviewCount rc : reviewCounts) {
            pendingReviewCounts.put(rc.getProductId(), rc.getCount());
        }

        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("activeProducts", activeProductsCount);
        model.addAttribute("lowStockProducts", lowStockProductsCount);
        model.addAttribute("pendingReviewCounts", pendingReviewCounts);
        model.addAttribute("page", "products");

        return "admin/product-manage";
    }

    @PostMapping("/create")
    public String create(@ModelAttribute SeafoodProduct seafoodProduct,
                         @RequestParam("categoryId") Long categoryId,
                         @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
                         @RequestParam(value = "primaryImageIndex", defaultValue = "0") Integer primaryImageIndex,
                         RedirectAttributes redirectAttributes) {
        try {
            seafoodProductService.save(seafoodProduct, categoryId, imageFiles, primaryImageIndex);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo sản phẩm thành công!");
            return "redirect:/admin/seafood-products";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/seafood-products";
        }
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @ModelAttribute SeafoodProduct seafoodProduct,
                         @RequestParam("categoryId") Long categoryId,
                         @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
                         @RequestParam(value = "primaryImageIndex", defaultValue = "0") Integer primaryImageIndex,
                         RedirectAttributes redirectAttributes) {
        try {
            seafoodProduct.setId(id);
            seafoodProductService.save(seafoodProduct, categoryId, imageFiles, primaryImageIndex);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật sản phẩm thành công!");
            return "redirect:/admin/seafood-products";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/seafood-products";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            seafoodProductService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa sản phẩm thành công!");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/seafood-products";
    }

    @GetMapping("/{id}/reviews")
    public String getProductReviews(@PathVariable Long id, Model model) {
        List<ProductReview> allReviews = reviewService.getAllReviewsForProduct(id);
        List<ProductReview> pendingReviews = allReviews.stream()
                .filter(r -> !r.getIsVisible())
                .toList();
        List<ProductReview> visibleReviews = allReviews.stream()
                .filter(ProductReview::getIsVisible)
                .toList();
        SeafoodProduct product = seafoodProductService.findById(id);

        model.addAttribute("product", product);
        model.addAttribute("allReviews", allReviews);
        model.addAttribute("pendingReviews", pendingReviews);
        model.addAttribute("visibleReviews", visibleReviews);
        model.addAttribute("pendingCount", pendingReviews.size());
        model.addAttribute("page", "products");

        return "admin/product-reviews";
    }

    @PostMapping("/{productId}/reviews/{reviewId}/approve")
    public String approveReview(@PathVariable Long productId, @PathVariable Long reviewId, RedirectAttributes redirectAttributes) {
        reviewService.approveReview(reviewId);
        redirectAttributes.addFlashAttribute("success", "Đánh giá đã được duyệt!");
        return "redirect:/admin/seafood-products/" + productId + "/reviews";
    }

    @PostMapping("/{productId}/reviews/{reviewId}/delete")
    public String deleteReview(@PathVariable Long productId, @PathVariable Long reviewId, RedirectAttributes redirectAttributes) {
        reviewService.deleteReview(reviewId);
        redirectAttributes.addFlashAttribute("success", "Đánh giá đã bị xóa!");
        return "redirect:/admin/seafood-products/" + productId + "/reviews";
    }
}

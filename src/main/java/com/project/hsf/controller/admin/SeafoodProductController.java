package com.project.hsf.controller.admin;

import com.project.hsf.entity.SeafoodProduct;
import com.project.hsf.service.impl.CategoryServiceImpl;
import com.project.hsf.service.impl.SeafoodProductServiceImpl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/seafood-products")
@RequiredArgsConstructor
public class SeafoodProductController {

    private final SeafoodProductServiceImpl seafoodProductService;
    private final CategoryServiceImpl categoryService;

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
        
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("activeProducts", activeProductsCount);
        model.addAttribute("lowStockProducts", lowStockProductsCount);
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
}

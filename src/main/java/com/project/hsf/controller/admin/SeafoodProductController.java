package com.project.hsf.controller.admin;

import com.project.hsf.entity.SeafoodProduct;
import com.project.hsf.repository.CategoryRepository;
import com.project.hsf.service.SeafoodProductService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestParam;

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

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        SeafoodProduct seafoodProduct = new SeafoodProduct();
        seafoodProduct.setActive(true);
        seafoodProduct.setFreshnessStatus("FRESH");
        model.addAttribute("seafoodProduct", seafoodProduct);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isEdit", false);
        return "seafood-products/form";
    }

    @PostMapping("/create")
    public String create(@ModelAttribute SeafoodProduct seafoodProduct,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
            @RequestParam(value = "primaryImageIndex", defaultValue = "0") Integer primaryImageIndex,
            RedirectAttributes redirectAttributes) {
        try {
            seafoodProductService.save(seafoodProduct, categoryId, imageFiles, primaryImageIndex);
            redirectAttributes.addFlashAttribute("successMessage", "Tao san pham thanh cong");
            return "redirect:/admin/seafood-products";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/seafood-products";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("seafoodProduct", seafoodProductService.findById(id));
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("isEdit", true);
            return "seafood-products/form"; // Wait, earlier it was "seafood-products/form", let's keep it. Actually it's probably "admin/product-manage" with edit mode? I will just leave the template paths as is.
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
            redirectAttributes.addFlashAttribute("successMessage", "Cap nhat san pham thanh cong");
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
            redirectAttributes.addFlashAttribute("successMessage", "Xoa san pham thanh cong");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/seafood-products";
    }
}

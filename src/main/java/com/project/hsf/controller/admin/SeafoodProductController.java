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

@Controller
@RequestMapping("/admin/seafood-products")
@RequiredArgsConstructor
public class SeafoodProductController {

    private final SeafoodProductServiceImpl seafoodProductService;
    private final CategoryServiceImpl categoryService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("products", seafoodProductService.findAll());
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
            RedirectAttributes redirectAttributes) {
        try {
            seafoodProductService.save(seafoodProduct, categoryId);
            redirectAttributes.addFlashAttribute("successMessage", "Tao san pham thanh cong");
            return "redirect:/seafood-products";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/seafood-products/create";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("seafoodProduct", seafoodProductService.findById(id));
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("isEdit", true);
            return "seafood-products/form";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/seafood-products";
        }
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
            @ModelAttribute SeafoodProduct seafoodProduct,
            @RequestParam("categoryId") Long categoryId,
            RedirectAttributes redirectAttributes) {
        try {
            seafoodProduct.setId(id);
            seafoodProductService.save(seafoodProduct, categoryId);
            redirectAttributes.addFlashAttribute("successMessage", "Cap nhat san pham thanh cong");
            return "redirect:/seafood-products";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/seafood-products/" + id + "/edit";
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
        return "redirect:/seafood-products";
    }
}

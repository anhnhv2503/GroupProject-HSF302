package com.project.hsf.controller.client;
/*
 * Copyright (c) 2026 vinhung. All rights reserved.
 */

import com.project.hsf.entity.SeafoodProduct;
import com.project.hsf.service.CategoryService;
import com.project.hsf.service.SeafoodProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class ProductClientController {

    private final SeafoodProductService productService;
    private final CategoryService categoryService;

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
    public String detail(@PathVariable Long id, Model model) {
        SeafoodProduct product = productService.findById(id);
        if (product == null) {
            return "redirect:/";
        }
        model.addAttribute("product", product);
        return "user/product-detail";
    }
}


package com.project.hsf.controller.admin;

import com.project.hsf.entity.Category;
import com.project.hsf.service.impl.CategoryServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryServiceImpl categoryService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("activeCategories", categoryService.findByActiveTrue());
        return "admin/category-manage";
    }

    @PostMapping("/save")
    public String save(Category category) {
        categoryService.save(category);
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        categoryService.deleteById(id);
        return "redirect:/admin/categories";
    }

    @PostMapping("/update")
    public String update(Category category) {
        categoryService.update(category);
        return "redirect:/admin/categories";
    }

}

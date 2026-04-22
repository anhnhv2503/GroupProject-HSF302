package com.project.hsf.controller.admin;

import com.project.hsf.entity.Banner;
import com.project.hsf.service.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/banners")
@RequiredArgsConstructor
public class BannerAdminController {

    private final BannerService bannerService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("banners", bannerService.findAll());
        model.addAttribute("page", "banners");
        return "admin/banner-manage";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Banner banner, RedirectAttributes redirectAttributes) {
        try {
            bannerService.save(banner);
            redirectAttributes.addFlashAttribute("successMessage", "Lưu banner thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi lưu banner: " + e.getMessage());
        }
        return "redirect:/admin/banners";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            bannerService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa banner thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa banner: " + e.getMessage());
        }
        return "redirect:/admin/banners";
    }

    @PostMapping("/toggle/{id}")
    public String toggle(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Banner banner = bannerService.findById(id);
            if (banner != null) {
                banner.setActive(!banner.getActive());
                bannerService.save(banner);
                redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái banner thành công!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/banners";
    }
}

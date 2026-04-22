package com.project.hsf.controller.client;
/*
 * Copyright (c) 2026 vinhung. All rights reserved.
 */

import com.project.hsf.service.BannerService;
import com.project.hsf.service.SeafoodProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final SeafoodProductService productService;
    private final BannerService bannerService;

    @GetMapping("/")
    public String index(Model model) {

        model.addAttribute("products", productService.search(null, null, true, null, "id", "desc"));
        model.addAttribute("newestProducts", productService.getNewestProducts());
        model.addAttribute("banners", bannerService.findActiveBanners());

        return "index";
    }

    @GetMapping("/home")
    public String home() {
        return "redirect:/";
    }
}

package com.project.hsf.controller.client;
/*
 * Copyright (c) 2026 vinhung. All rights reserved.
 */

import com.project.hsf.dto.CustomOauth2User;
import com.project.hsf.entity.User;
import com.project.hsf.service.BannerService;
import com.project.hsf.service.SeafoodProductService;
import com.project.hsf.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final SeafoodProductService productService;
    private final BannerService bannerService;
    private final UserService userService;

    @GetMapping("/")
    public String index(Model model, Authentication authentication) {

        model.addAttribute("products", productService.search(null, null, true, null, "id", "desc"));
        model.addAttribute("newestProducts", productService.getNewestProducts());
        model.addAttribute("banners", bannerService.findActiveBanners());

        if (authentication != null && authentication.isAuthenticated()) {
            User user = userService.findByUsername(authentication.getName());
            if (user != null) {
                model.addAttribute("canAddToCart", userService.canUserAddToCart(user.getId()));
            }
        }

        return "index";
    }

    @GetMapping("/home")
    public String home(Authentication authentication) {

        System.out.println("Auth = " + authentication.getName());

        if (authentication != null) {
            System.out.println("Principal class = "
                    + authentication.getPrincipal().getClass());
        }
        return "redirect:/";
    }
}

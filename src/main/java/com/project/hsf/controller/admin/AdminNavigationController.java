package com.project.hsf.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminNavigationController {

    @GetMapping("/manage-orders")
    public String manageOrderPage(){
        return "admin/manage-orders";
    }
}

package com.project.hsf.controller;

import com.project.hsf.dto.RegisterDTO;
import com.project.hsf.service.impl.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserServiceImpl userService;

    @GetMapping("/login")
    public String showLoginForm() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        // IMPORTANT: must match th:object="${registerDTO}" in template
        model.addAttribute("registerDTO", new RegisterDTO());
        return "auth/register";
    }

    @PostMapping("/register-user")
    public String processRegister(
            @ModelAttribute("registerDTO") RegisterDTO registerDTO,
            Model model) {
        try {
            // Basic validation
            if (registerDTO.getUsername() == null || registerDTO.getUsername().trim().isEmpty()) {
                model.addAttribute("error", "Tên đăng nhập không được để trống.");
                model.addAttribute("registerDTO", registerDTO);
                return "auth/register";
            }
            if (registerDTO.getEmail() == null || registerDTO.getEmail().trim().isEmpty()) {
                model.addAttribute("error", "Email không được để trống.");
                model.addAttribute("registerDTO", registerDTO);
                return "auth/register";
            }
            if (registerDTO.getPassword() == null || registerDTO.getPassword().length() < 6) {
                model.addAttribute("error", "Mật khẩu phải có ít nhất 6 ký tự.");
                model.addAttribute("registerDTO", registerDTO);
                return "auth/register";
            }
            if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
                model.addAttribute("error", "Mật khẩu xác nhận không khớp.");
                model.addAttribute("registerDTO", registerDTO);
                return "auth/register";
            }

            userService.registerUser(registerDTO);
            return "redirect:/login?registerSuccess=true";

        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("registerDTO", registerDTO);
            return "auth/register";
        } catch (Exception e) {
            model.addAttribute("error", "Đã xảy ra lỗi. Vui lòng thử lại sau.");
            model.addAttribute("registerDTO", registerDTO);
            return "auth/register";
        }
    }
}
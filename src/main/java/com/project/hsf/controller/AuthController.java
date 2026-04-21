package com.project.hsf.controller;

import com.project.hsf.dto.RegisterDTO;
import jakarta.validation.Valid;
import com.project.hsf.service.impl.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserServiceImpl userService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

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
            @Valid @ModelAttribute("registerDTO") RegisterDTO registerDTO,
            BindingResult bindingResult,
            Model model) {
        try {
            Map<String, String> fieldErrors = new LinkedHashMap<>();

            if (bindingResult.hasErrors()) {
                for (FieldError fieldError : bindingResult.getFieldErrors()) {
                    fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
                }

                // @AssertTrue on DTO creates an object-level error, map it to confirmPassword for UI display.
                if (bindingResult.hasGlobalErrors() && !fieldErrors.containsKey("confirmPassword")) {
                    fieldErrors.put("confirmPassword", bindingResult.getGlobalError().getDefaultMessage());
                }
            }

            if (!fieldErrors.containsKey("username") && userService.existsByUsername(registerDTO.getUsername())) {
                fieldErrors.put("username", "Tên đăng nhập đã tồn tại.");
            }

            if (!fieldErrors.containsKey("email") && userService.existsByEmail(registerDTO.getEmail())) {
                fieldErrors.put("email", "Email đã được sử dụng.");
            }

            if (!fieldErrors.isEmpty()) {
                model.addAttribute("error", "Vui lòng kiểm tra lại thông tin đăng ký.");
                model.addAttribute("fieldErrors", fieldErrors);
                model.addAttribute("registerDTO", registerDTO);
                return "auth/register";
            }

            userService.registerUser(registerDTO);
            return "redirect:/login?registerSuccess=true";

        } catch (IllegalArgumentException e) {
            Map<String, String> fieldErrors = new LinkedHashMap<>();
            String message = e.getMessage() != null ? e.getMessage() : "Dữ liệu không hợp lệ.";

            if (message.toLowerCase().contains("username")) {
                fieldErrors.put("username", message);
            } else if (message.toLowerCase().contains("email")) {
                fieldErrors.put("email", message);
            }

            model.addAttribute("error", message);
            if (!fieldErrors.isEmpty()) {
                model.addAttribute("fieldErrors", fieldErrors);
            }
            model.addAttribute("registerDTO", registerDTO);
            return "auth/register";
        } catch (Exception e) {
            model.addAttribute("error", "Đã xảy ra lỗi. Vui lòng thử lại sau.");
            model.addAttribute("registerDTO", registerDTO);
            return "auth/register";
        }
    }
}


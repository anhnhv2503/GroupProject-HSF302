package com.project.hsf.controller.client;

import com.project.hsf.entity.User;
import com.project.hsf.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public String viewProfile(Authentication authentication, Model model) {
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        model.addAttribute("user", user);
        return "user/profile";
    }

    @PostMapping("/update")
    public String updateProfile(
            Authentication authentication,
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String phone,
            Model model) {
        String currentUsername = authentication.getName();
        User user = userService.findByUsername(currentUsername);

        if (user != null) {
            if (username != null && !username.trim().isEmpty() && !username.equals(currentUsername)) {
                if (userService.existsByUsername(username)) {
                    model.addAttribute("error", "Tên đăng nhập đã tồn tại");
                    model.addAttribute("user", user);
                    return "user/profile";
                }
                user.setUsername(username);
            }
            if (email != null && !email.trim().isEmpty() && !email.equals(user.getEmail())) {
                if (userService.existsByEmail(email)) {
                    model.addAttribute("error", "Email đã được sử dụng");
                    model.addAttribute("user", user);
                    return "user/profile";
                }
                user.setEmail(email);
            }
            user.setFullName(fullName);
            user.setPhone(phone);
            userService.updateUser(user);
            model.addAttribute("success", "Cập nhật thông tin thành công!");
        }

        model.addAttribute("user", userService.findByUsername(user.getUsername()));
        return "user/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(
            Authentication authentication,
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Model model) {
        String username = authentication.getName();
        User user = userService.findByUsername(username);

        if (user == null) {
            model.addAttribute("error", "Không tìm thấy người dùng");
            model.addAttribute("user", user);
            return "user/profile";
        }

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            model.addAttribute("error", "Mật khẩu hiện tại không đúng");
            model.addAttribute("user", user);
            return "user/profile";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu xác nhận không khớp");
            model.addAttribute("user", user);
            return "user/profile";
        }

        if (newPassword.length() < 6) {
            model.addAttribute("error", "Mật khẩu mới phải có ít nhất 6 ký tự");
            model.addAttribute("user", user);
            return "user/profile";
        }

        userService.changePassword(user, newPassword);
        model.addAttribute("success", "Đổi mật khẩu thành công!");
        model.addAttribute("user", userService.findByUsername(username));
        return "user/profile";
    }
}
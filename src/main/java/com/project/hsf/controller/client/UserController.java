package com.project.hsf.controller.client;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.project.hsf.entity.User;
import com.project.hsf.entity.UserAddress;
import com.project.hsf.service.AddressService;
import com.project.hsf.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AddressService addressService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public String viewProfile(Authentication authentication, Model model) {
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        model.addAttribute("user", user);
        return "user/profile";
    }

    @GetMapping("/address")
    public String viewAddresses(Authentication authentication, Model model) {
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        List<UserAddress> addresses = addressService.getAddressesByUser(user);
        model.addAttribute("user", user);
        model.addAttribute("addresses", addresses);
        return "user/address";
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

    @PostMapping("/address/create")
    public String createAddress(
            Authentication authentication,
            @RequestParam String phone,
            @RequestParam String addressLine,
            @RequestParam(required = false) String ward,
            @RequestParam(required = false) String city,
            @RequestParam(required = false, defaultValue = "false") Boolean isDefault,
            Model model) {
        String username = authentication.getName();
        User user = userService.findByUsername(username);

        addressService.createAddress(user, phone, addressLine, ward, city, isDefault);
        
        model.addAttribute("user", user);
        model.addAttribute("success", "Thêm địa chỉ thành công!");
        model.addAttribute("addresses", addressService.getAddressesByUser(user));
        return "user/address";
    }

    @PostMapping("/address/update")
    public String updateAddress(
            Authentication authentication,
            @RequestParam Long addressId,
            @RequestParam String phone,
            @RequestParam String addressLine,
            @RequestParam(required = false) String ward,
            @RequestParam(required = false) String city,
            @RequestParam(required = false, defaultValue = "false") Boolean isDefault,
            Model model) {
        String username = authentication.getName();
        User user = userService.findByUsername(username);

        addressService.updateAddress(addressId, user, phone, addressLine, ward, city, isDefault);
        
        model.addAttribute("user", user);
        model.addAttribute("success", "Cập nhật địa chỉ thành công!");
        model.addAttribute("addresses", addressService.getAddressesByUser(user));
        return "user/address";
    }

    @PostMapping("/address/delete")
    public String deleteAddress(
            Authentication authentication,
            @RequestParam Long addressId,
            Model model) {
        String username = authentication.getName();
        User user = userService.findByUsername(username);

        addressService.deleteAddress(addressId, user);
        
        model.addAttribute("user", user);
        model.addAttribute("success", "Xóa địa chỉ thành công!");
        model.addAttribute("addresses", addressService.getAddressesByUser(user));
        return "user/address";
    }

    @PostMapping("/address/set-default")
    public String setDefaultAddress(
            Authentication authentication,
            @RequestParam Long addressId,
            Model model) {
        String username = authentication.getName();
        User user = userService.findByUsername(username);

        addressService.setDefaultAddress(addressId, user);
        
        model.addAttribute("user", user);
        model.addAttribute("success", "Đặt địa chỉ mặc định thành công!");
        model.addAttribute("addresses", addressService.getAddressesByUser(user));
        return "user/address";
    }
}
package com.project.hsf.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDTO {
    @NotBlank(message = "Tên đăng nhập không được để trống.")
    @Size(min = 4, max = 30, message = "Tên đăng nhập 4-30 ký tự.")
    @Pattern(regexp = "^[a-zA-Z0-9._-]{4,30}$", message = "Tên đăng nhập chỉ gồm chữ, số, ., _, -.")
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống.")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự.")
    private String password;

    @NotBlank(message = "Vui lòng nhập lại mật khẩu.")
    private String confirmPassword;

    @NotBlank(message = "Email không được để trống.")
    @Email(message = "Email không đúng định dạng.")
    private String email;

    @NotBlank(message = "Họ và tên không được để trống.")
    @Size(max = 150, message = "Họ và tên không vượt quá 150 ký tự.")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống.")
    @Pattern(regexp = "^[0-9]{9,11}$", message = "Số điện thoại phải gồm 9-11 chữ số.")
    private String phone;

    @AssertTrue(message = "Mật khẩu xác nhận không khớp.")
    public boolean isPasswordConfirmed() {
        if (password == null || confirmPassword == null) {
            return false;
        }
        return password.equals(confirmPassword);
    }
}

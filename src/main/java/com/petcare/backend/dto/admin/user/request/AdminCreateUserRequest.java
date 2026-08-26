package com.petcare.backend.dto.admin.user.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminCreateUserRequest {
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 255, message = "Email không được quá 255 ký tự")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, max = 32, message = "Mật khẩu phải từ 8 đến 32 ký tự")
    private String password;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 255, message = "Họ tên không được quá 255 ký tự")
    private String fullName;

    @Size(max = 50, message = "Username không được quá 50 ký tự")
    private String username;

    @Pattern(regexp = "^$|\\d{10}", message = "Số điện thoại phải gồm đúng 10 chữ số")
    private String phoneNumber;

    private String role;
    private String status;
    private String avatarUrl;
}

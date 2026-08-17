package com.petcare.backend.dto.admin.user.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUpdateUserRequest {
    @Email(message = "Email không hợp lệ")
    @Size(max = 255, message = "Email không được quá 255 ký tự")
    private String email;

    @Size(max = 255, message = "Họ tên không được quá 255 ký tự")
    private String fullName;

    @Size(max = 50, message = "Username không được quá 50 ký tự")
    private String username;

    @Pattern(regexp = "^$|\\d{10}", message = "Số điện thoại phải gồm đúng 10 chữ số")
    private String phoneNumber;

    @Size(max = 150, message = "Bio không được quá 150 ký tự")
    private String bio;

    private LocalDate dateOfBirth;

    @Size(max = 150, message = "Vị trí không được quá 150 ký tự")
    private String location;

    private String role;
    private String status;
    private Boolean emailVerified;
    private String avatarUrl;
}

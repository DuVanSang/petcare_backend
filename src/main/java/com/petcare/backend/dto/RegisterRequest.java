package com.petcare.backend.dto;

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
public class RegisterRequest {
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, max = 32, message = "Mật khẩu phải từ 8 đến 32 ký tự")
    private String password;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100, message = "Họ tên không được vượt quá 100 ký tự")
    private String fullName;

    @Pattern(regexp = "^$|\\d{10}", message = "Số điện thoại phải gồm 10 chữ số")
    private String phoneNumber;

    private String deviceId;

    @Size(max = 255, message = "Tên thiết bị không được vượt quá 255 ký tự")
    private String deviceName;

    @Pattern(regexp = "^$|ios|android|web", message = "Loại thiết bị phải là ios, android hoặc web")
    private String deviceType;

    private String deviceToken;

    private Boolean notificationEnabled;

    @Size(max = 50, message = "Phiên bản ứng dụng không được vượt quá 50 ký tự")
    private String appVersion;

    @Size(max = 100, message = "Phiên bản hệ điều hành không được vượt quá 100 ký tự")
    private String osVersion;
}

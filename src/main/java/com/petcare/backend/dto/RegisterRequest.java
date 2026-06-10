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
    @NotBlank(message = "Email khong duoc de trong")
    @Email(message = "Email khong dung dinh dang")
    private String email;

    @NotBlank(message = "Mat khau khong duoc de trong")
    @Size(min = 8, max = 32, message = "Mat khau phai tu 8 den 32 ky tu")
    private String password;

    @NotBlank(message = "Ho ten khong duoc de trong")
    @Size(max = 100, message = "Ho ten khong duoc vuot qua 100 ky tu")
    private String fullName;

    @Pattern(regexp = "^$|\\d{10}", message = "So dien thoai phai gom 10 chu so")
    private String phoneNumber;

    private String deviceToken;

    @Pattern(regexp = "^$|ios|android|web", message = "Device type phai la ios, android hoac web")
    private String deviceType;
}

package com.petcare.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyEmailRequest {
    @Email(message = "Email khong dung dinh dang")
    @NotBlank(message = "Email khong duoc de trong")
    private String email;

    @NotBlank(message = "Ma OTP khong duoc de trong")
    @Pattern(regexp = "\\d{6}", message = "Ma OTP phai gom 6 chu so")
    private String otpCode;
}

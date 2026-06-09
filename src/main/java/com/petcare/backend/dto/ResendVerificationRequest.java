package com.petcare.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResendVerificationRequest {
    @Email(message = "Email khong dung dinh dang")
    @NotBlank(message = "Email khong duoc de trong")
    private String email;
}

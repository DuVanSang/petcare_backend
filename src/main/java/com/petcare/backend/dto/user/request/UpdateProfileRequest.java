package com.petcare.backend.dto.user.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {
    @Size(max = 100, message = "Họ tên không được vượt quá 100 ký tự")
    private String fullName;

    @Pattern(regexp = "^$|\\d{10}", message = "Số điện thoại phải gồm 10 chữ số")
    private String phoneNumber;

    @Size(max = 1024, message = "Đường dẫn ảnh đại diện không được vượt quá 1024 ký tự")
    private String avatarUrl;
}

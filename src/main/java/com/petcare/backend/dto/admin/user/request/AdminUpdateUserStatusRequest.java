package com.petcare.backend.dto.admin.user.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUpdateUserStatusRequest {
    @NotBlank(message = "Trạng thái không được để trống")
    private String status;
}

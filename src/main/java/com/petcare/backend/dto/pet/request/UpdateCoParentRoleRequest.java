package com.petcare.backend.dto.pet.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCoParentRoleRequest {
    @NotBlank(message = "Role is required")
    private String role;
}

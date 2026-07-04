package com.petcare.backend.dto.admin.category.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminCreateSpeciesRequest {
    @NotBlank(message = "Tên loài không được để trống")
    @Size(max = 50, message = "Tên loài không được quá 50 ký tự")
    private String name;

    @Size(max = 255, message = "URL icon không được quá 255 ký tự")
    private String iconUrl;
}

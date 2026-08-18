package com.petcare.backend.dto.admin.category.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminCreateBreedRequest {
    @NotNull(message = "Loài không được để trống")
    private Long speciesId;

    @NotBlank(message = "Tên giống không được để trống")
    @Size(max = 100, message = "Tên giống không được quá 100 ký tự")
    private String name;

    private Boolean active;
}

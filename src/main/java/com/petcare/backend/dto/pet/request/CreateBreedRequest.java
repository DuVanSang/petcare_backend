package com.petcare.backend.dto.pet.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateBreedRequest {

    @Schema(description = "Tên giống hiển thị trên dropdown", example = "Golden Retriever")
    @NotBlank(message = "Tên giống không được để trống")
    @Size(max = 100, message = "Tên giống không được quá 100 ký tự")
    private String name;
}

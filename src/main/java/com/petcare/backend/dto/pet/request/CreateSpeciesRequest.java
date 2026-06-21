package com.petcare.backend.dto.pet.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateSpeciesRequest {

    @Schema(description = "Tên loài hiển thị trên dropdown", example = "Chó")
    @NotBlank(message = "Tên loài không được để trống")
    @Size(max = 50, message = "Tên loài không được quá 50 ký tự")
    private String name;

    @Schema(description = "URL icon (tuỳ chọn)", example = "https://cdn.example.com/dog.png")
    @Size(max = 255, message = "URL icon không được quá 255 ký tự")
    private String iconUrl;
}

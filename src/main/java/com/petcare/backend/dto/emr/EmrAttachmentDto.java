package com.petcare.backend.dto.emr;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmrAttachmentDto {

    @NotBlank(message = "Tên tệp không được để trống")
    @Size(max = 255, message = "Tên tệp không được quá 255 ký tự")
    @JsonProperty("file_name")
    @Schema(description = "Tên tệp gốc", example = "tai_viem.jpg")
    private String fileName;

    @NotBlank(message = "URL tệp không được để trống")
    @Size(max = 255, message = "URL tệp không được quá 255 ký tự")
    @JsonProperty("file_url")
    @Schema(description = "URL tệp trên cloud storage", example = "https://s3.com/tai_viem.jpg")
    private String fileUrl;

    @NotBlank(message = "Loại tệp không được để trống")
    @Size(max = 50, message = "Loại tệp không được quá 50 ký tự")
    @JsonProperty("file_type")
    @Schema(description = "MIME type", example = "image/jpeg")
    private String fileType;
}

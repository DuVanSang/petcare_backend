package com.petcare.backend.dto.pet.request;

import com.petcare.backend.model.Pet;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class CreatePetRequest {

    @NotBlank(message = "Tên thú cưng không được để trống")
    @Size(max = 100, message = "Tên thú cưng không được quá 100 ký tự")
    private String name;

    @Schema(description = "URL ảnh đại diện (Step 1)", example = "https://cdn.example.com/pets/luna.jpg")
    @Size(max = 255, message = "URL ảnh không được quá 255 ký tự")
    private String avatarUrl;

    @Schema(description = "ID loài — lấy từ GET /categories/species", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Vui lòng chọn loài thú cưng")
    private Long speciesId;

    @Schema(description = "ID giống — lấy từ GET /categories/species/{speciesId}/breeds", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Vui lòng chọn giống thú cưng")
    private Long breedId;

    @Schema(description = "Tên giống tự nhập. Bắt buộc khi breedId là mục \"Khác\" hoặc \"Hỗn hợp / Không rõ\" (otherOption=true)", example = "Mèo Ba Tư lai")
    @Size(max = 100, message = "Tên giống tự nhập không được quá 100 ký tự")
    private String customBreedName;

    private Pet.Gender gender;

    private LocalDate dateOfBirth;

    private Integer estimatedAgeMonths;

    @DecimalMin(value = "0.01", message = "Cân nặng phải lớn hơn 0")
    private BigDecimal currentWeight;

    private String colorFeatures;

    private Pet.SpayedStatus spayedStatus;

    @Schema(description = "Trạng thái ban đầu (Step 3)", example = "active")
    private Pet.PetStatus status;

    @Schema(description = "Danh sách dị ứng (Step 3)", example = "[\"Dị ứng thịt gà\"]")
    private List<String> allergies;

    @Schema(description = "Bệnh lý mãn tính / hiện tại (Step 3)", example = "[\"Viêm da\"]")
    private List<String> medicalConditions;

    private String notes;
}

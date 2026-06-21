package com.petcare.backend.dto.pet.request;

import com.petcare.backend.model.Pet;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class UpdatePetRequest {

    @Size(max = 100, message = "Tên thú cưng không được quá 100 ký tự")
    private String name;

    @Schema(description = "ID loài — lấy từ GET /categories/species", example = "1")
    private Long speciesId;

    @Schema(description = "ID giống — lấy từ GET /categories/species/{speciesId}/breeds", example = "1")
    private Long breedId;

    @Schema(description = "Tên giống tự nhập khi chọn mục Khác (otherOption=true)", example = "Chó ta lai")
    @Size(max = 100, message = "Tên giống tự nhập không được quá 100 ký tự")
    private String customBreedName;

    private Pet.Gender gender;

    private LocalDate dateOfBirth;

    private Integer estimatedAgeMonths;

    @DecimalMin(value = "0.01", message = "Cân nặng phải lớn hơn 0")
    private BigDecimal currentWeight;

    private String colorFeatures;

    private Pet.SpayedStatus spayedStatus;

    private Pet.PetStatus status;

    private String notes;
}

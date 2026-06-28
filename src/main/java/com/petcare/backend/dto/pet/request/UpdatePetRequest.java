package com.petcare.backend.dto.pet.request;

import com.petcare.backend.model.Pet;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class UpdatePetRequest {
    private static final LocalDate MIN_PET_BIRTH_DATE = LocalDate.of(1900, 1, 1);

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

    @PastOrPresent(message = "Ngày sinh không được ở tương lai")
    private LocalDate dateOfBirth;

    @Min(value = 0, message = "Tuổi ước tính không được âm")
    private Integer estimatedAgeMonths;

    @DecimalMin(value = "0.01", message = "Cân nặng phải lớn hơn 0")
    private BigDecimal currentWeight;

    private String colorFeatures;

    private Pet.SpayedStatus spayedStatus;

    private Pet.PetStatus status;

    private String notes;

    private List<String> allergies;

    private List<String> medicalConditions;

    @AssertTrue(message = "Ngày sinh thú cưng không hợp lệ")
    public boolean isValidDateOfBirth() {
        return dateOfBirth == null
                || (!dateOfBirth.isBefore(MIN_PET_BIRTH_DATE) && !dateOfBirth.isAfter(LocalDate.now()));
    }
}

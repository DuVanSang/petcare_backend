package com.petcare.backend.dto.pet.request;

import com.petcare.backend.model.Pet;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePetRequest {
    @NotBlank(message = "Tên thú cưng không được để trống")
    @Size(max = 100, message = "Tên thú cưng không được quá 100 ký tự")
    private String name;

    @Size(max = 500, message = "URL ảnh không được quá 500 ký tự")
    private String avatarUrl;

    @NotNull(message = "Vui lòng chọn loài thú cưng")
    private Long speciesId;

    @NotNull(message = "Vui lòng chọn giống thú cưng")
    private Long breedId;

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
    private List<String> allergies;
    private List<String> medicalConditions;

    private String notes;
}

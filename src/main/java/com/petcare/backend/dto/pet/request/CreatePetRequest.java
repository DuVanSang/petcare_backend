package com.petcare.backend.dto.pet.request;

import com.petcare.backend.model.Pet;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreatePetRequest {

    @NotBlank(message = "Tên thú cưng không được để trống")
    @Size(max = 100, message = "Tên thú cưng không được quá 100 ký tự")
    private String name;

    private Long speciesId;

    private Long breedId;

    private Pet.Gender gender;

    private LocalDate dateOfBirth;

    private Integer estimatedAgeMonths;

    @DecimalMin(value = "0.01", message = "Cân nặng phải lớn hơn 0")
    private BigDecimal currentWeight;

    private String colorFeatures;

    private Pet.SpayedStatus spayedStatus;

    private String microchipNumber;

    private String notes;
}

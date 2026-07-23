package com.petcare.backend.dto.pet.request;

import static org.assertj.core.api.Assertions.assertThat;

import com.petcare.backend.model.Pet;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class PetRequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void createPetRequest_ValidatesRequiredFieldsBoundariesAndBirthDateBusinessRule() {
        CreatePetRequest request = new CreatePetRequest();
        request.setName("Milo"); request.setSpeciesId(1L); request.setBreedId(null); request.setCustomBreedName("");
        request.setGender(Pet.Gender.male); request.setDateOfBirth(LocalDate.now()); request.setEstimatedAgeMonths(0);
        request.setCurrentWeight(new BigDecimal("0.01")); request.setColorFeatures("brown"); request.setSpayedStatus(Pet.SpayedStatus.intact);
        request.setStatus(Pet.PetStatus.active); request.setAllergies(List.of("dust")); request.setMedicalConditions(List.of()); request.setNotes(null);
        assertThat(validator.validate(request)).isEmpty(); assertThat(request.isValidDateOfBirth()).isTrue();
        request.setDateOfBirth(LocalDate.of(1899, 12, 31)); assertThat(request.isValidDateOfBirth()).isFalse();
        request.setDateOfBirth(LocalDate.now().plusDays(1)); assertThat(request.isValidDateOfBirth()).isFalse();
        request.setDateOfBirth(null); assertThat(request.isValidDateOfBirth()).isTrue();
        request.setName(" "); request.setSpeciesId(null); request.setEstimatedAgeMonths(-1); request.setCurrentWeight(BigDecimal.ZERO);
        assertThat(validator.validate(request)).extracting(v -> v.getPropertyPath().toString())
                .contains("name", "speciesId", "estimatedAgeMonths", "currentWeight");
    }

    @Test
    void updatePetRequest_ValidatesOptionalFieldsBoundariesAndBirthDateBusinessRule() {
        UpdatePetRequest request = new UpdatePetRequest();
        assertThat(validator.validate(request)).isEmpty(); assertThat(request.isValidDateOfBirth()).isTrue();
        request.setName("Milo"); request.setSpeciesId(0L); request.setBreedId(-1L); request.setCustomBreedName("custom");
        request.setGender(Pet.Gender.female); request.setDateOfBirth(LocalDate.of(1900, 1, 1)); request.setEstimatedAgeMonths(0);
        request.setCurrentWeight(new BigDecimal("0.01")); request.setColorFeatures(""); request.setSpayedStatus(Pet.SpayedStatus.spayed);
        request.setStatus(Pet.PetStatus.active); request.setNotes("note"); request.setAllergies(List.of()); request.setMedicalConditions(List.of("skin"));
        assertThat(validator.validate(request)).isEmpty(); assertThat(request.isValidDateOfBirth()).isTrue();
        request.setDateOfBirth(LocalDate.of(1899, 12, 31)); assertThat(request.isValidDateOfBirth()).isFalse();
        request.setDateOfBirth(LocalDate.now().plusDays(1)); assertThat(request.isValidDateOfBirth()).isFalse();
        request.setCurrentWeight(BigDecimal.ZERO); request.setEstimatedAgeMonths(-1); request.setName("x".repeat(101));
        assertThat(validator.validate(request)).extracting(v -> v.getPropertyPath().toString())
                .contains("name", "estimatedAgeMonths", "currentWeight", "dateOfBirth");
    }

    @Test
    void vaccinationHistoryRequest_ValidatesEnumPartitionsAndConsistencyRules() {
        VaccinationHistoryRequest request = new VaccinationHistoryRequest();
        assertThat(request.isConsistent()).isTrue();
        request.setSeriesCode("DHPP_1"); request.setStatus(VaccinationHistoryRequest.HistoryStatus.NONE); request.setCompletedDoses(0); request.setLastVaccinationDate(null);
        assertThat(request.isConsistent()).isTrue();
        request.setCompletedDoses(1); assertThat(request.isConsistent()).isFalse();
        request.setStatus(VaccinationHistoryRequest.HistoryStatus.PARTIAL); request.setCompletedDoses(1); request.setLastVaccinationDate(LocalDate.now()); assertThat(request.isConsistent()).isTrue();
        request.setCompletedDoses(0); assertThat(request.isConsistent()).isFalse();
        request.setCompletedDoses(1); request.setLastVaccinationDate(null); assertThat(request.isConsistent()).isFalse();
        request.setStatus(VaccinationHistoryRequest.HistoryStatus.COMPLETE); request.setCompletedDoses(null); assertThat(request.isConsistent()).isTrue();
        request.setCompletedDoses(0); assertThat(request.isConsistent()).isFalse();
        request.setStatus(VaccinationHistoryRequest.HistoryStatus.UNKNOWN); assertThat(request.isConsistent()).isTrue();
        request.setSeriesCode("bad code"); request.setCompletedDoses(-1); request.setLastVaccinationDate(LocalDate.now().plusDays(1));
        assertThat(validator.validate(request)).extracting(v -> v.getPropertyPath().toString())
                .contains("seriesCode", "completedDoses", "lastVaccinationDate");
    }
}

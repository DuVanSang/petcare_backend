package com.petcare.backend.dto.pet.response;

import com.petcare.backend.model.Breed;
import com.petcare.backend.util.BreedCategoryHelper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BreedResponse {

    private Long id;
    private Long speciesId;
    private String name;
    private Boolean active;

    @Schema(description = "true = mục \"Khác\" / \"Hỗn hợp / Không rõ\", UI cần hiện ô nhập giống tự do", example = "false")
    private boolean otherOption;

    public static BreedResponse from(Breed breed) {
        BreedResponse dto = new BreedResponse();
        dto.setId(breed.getId());
        dto.setSpeciesId(breed.getSpecies().getId());
        dto.setName(breed.getName());
        dto.setActive(breed.getActive());
        dto.setOtherOption(BreedCategoryHelper.isOtherBreed(breed.getName()));
        return dto;
    }
}

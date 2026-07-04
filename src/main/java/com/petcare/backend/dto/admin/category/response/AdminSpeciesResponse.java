package com.petcare.backend.dto.admin.category.response;

import com.petcare.backend.model.Species;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminSpeciesResponse {
    private Long id;
    private String name;
    private String iconUrl;
    private Boolean active;

    public static AdminSpeciesResponse from(Species species) {
        return AdminSpeciesResponse.builder()
                .id(species.getId())
                .name(species.getName())
                .iconUrl(species.getIconUrl())
                .active(species.getActive())
                .build();
    }
}

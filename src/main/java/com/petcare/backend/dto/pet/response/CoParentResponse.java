package com.petcare.backend.dto.pet.response;

import com.petcare.backend.model.PetCoParent;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CoParentResponse {

    private Long id;
    private Long userId;
    private String fullName;
    private String email;
    private String avatarUrl;
    private String role;
    private LocalDateTime joinedAt;

    public static CoParentResponse from(PetCoParent coParent) {
        CoParentResponse dto = new CoParentResponse();
        dto.setId(coParent.getId());
        dto.setUserId(coParent.getUser().getId());
        dto.setFullName(coParent.getUser().getFullName());
        dto.setEmail(coParent.getUser().getEmail());
        dto.setAvatarUrl(coParent.getUser().getAvatarUrl());
        dto.setRole(coParent.getRole() != null ? coParent.getRole().name() : null);
        dto.setJoinedAt(coParent.getJoinedAt());
        return dto;
    }
}

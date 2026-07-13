package com.petcare.backend.dto.admin.pet.response;

import com.petcare.backend.model.PetCoParent;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminPetCoParentResponse {
    private Long id;
    private Long userId;
    private String fullName;
    private String email;
    private String role;
    private Long invitedBy;
    private LocalDateTime joinedAt;

    public static AdminPetCoParentResponse from(PetCoParent coParent) {
        return AdminPetCoParentResponse.builder()
                .id(coParent.getId())
                .userId(coParent.getUser() == null ? null : coParent.getUser().getId())
                .fullName(coParent.getUser() == null ? null : coParent.getUser().getFullName())
                .email(coParent.getUser() == null ? null : coParent.getUser().getEmail())
                .role(coParent.getRole() == null ? null : coParent.getRole().name())
                .invitedBy(coParent.getInvitedBy() == null ? null : coParent.getInvitedBy().getId())
                .joinedAt(coParent.getJoinedAt())
                .build();
    }
}

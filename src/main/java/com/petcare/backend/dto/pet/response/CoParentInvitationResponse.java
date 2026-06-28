package com.petcare.backend.dto.pet.response;

import com.petcare.backend.model.CoParentInvitation;
import com.petcare.backend.model.User;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class CoParentInvitationResponse {
    private Long id; private Long petId; private String petName; private String petAvatarUrl;
    private Long inviterId; private String inviterName; private String inviterEmail; private String inviterAvatarUrl;
    private Long inviteeUserId; private String inviteeEmail; private String inviteeName; private String inviteeAvatarUrl;
    private String role; private String status;
    private LocalDateTime expiresAt; private LocalDateTime createdAt;
    private LocalDateTime acceptedAt; private LocalDateTime declinedAt; private LocalDateTime revokedAt;

    public static CoParentInvitationResponse from(CoParentInvitation item) {
        User invitee = item.getInviteeUser();
        return CoParentInvitationResponse.builder()
                .id(item.getId()).petId(item.getPet().getId()).petName(item.getPet().getName())
                .petAvatarUrl(item.getPet().getAvatarUrl())
                .inviterId(item.getInviter().getId()).inviterName(item.getInviter().getFullName())
                .inviterEmail(item.getInviter().getEmail()).inviterAvatarUrl(item.getInviter().getAvatarUrl())
                .inviteeUserId(invitee == null ? null : invitee.getId()).inviteeEmail(item.getInviteeEmail())
                .inviteeName(invitee == null ? null : invitee.getFullName())
                .inviteeAvatarUrl(invitee == null ? null : invitee.getAvatarUrl())
                .role(item.getRole().name()).status(item.getStatus().name())
                .expiresAt(item.getExpiresAt()).createdAt(item.getCreatedAt())
                .acceptedAt(item.getAcceptedAt()).declinedAt(item.getDeclinedAt()).revokedAt(item.getRevokedAt())
                .build();
    }
}

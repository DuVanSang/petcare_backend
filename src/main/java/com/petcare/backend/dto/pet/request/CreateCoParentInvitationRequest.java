package com.petcare.backend.dto.pet.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateCoParentInvitationRequest {
    @NotBlank @Email private String inviteeEmail;
    @NotBlank private String role;
}

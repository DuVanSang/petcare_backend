package com.petcare.backend.dto.pet.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AcceptInvitationRequest {

    @NotBlank(message = "Mã mời không được để trống")
    private String inviteCode;
}

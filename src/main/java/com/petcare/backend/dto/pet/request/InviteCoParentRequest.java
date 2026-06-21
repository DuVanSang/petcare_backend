package com.petcare.backend.dto.pet.request;

import com.petcare.backend.model.PetCoParent;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InviteCoParentRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String inviteeEmail;

    @NotNull(message = "Role không được để trống")
    private PetCoParent.CoParentRole role;
}

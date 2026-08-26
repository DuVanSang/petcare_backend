package com.petcare.backend.dto.locket.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactPetMomentRequest {
    @NotBlank(message = "Emoji không được để trống")
    private String emoji;
}

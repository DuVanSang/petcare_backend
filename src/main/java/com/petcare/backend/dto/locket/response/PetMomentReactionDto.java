package com.petcare.backend.dto.locket.response;

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
public class PetMomentReactionDto {
    private Long id;
    private Long userId;
    private String userName;
    private String userAvatarUrl;
    private String emoji;
    private String createdAt;
}

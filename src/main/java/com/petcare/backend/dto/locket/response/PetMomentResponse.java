package com.petcare.backend.dto.locket.response;

import java.util.List;
import java.util.Map;
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
public class PetMomentResponse {
    private Long id;
    private Long petId;
    private String petName;
    private String petAvatarUrl;
    private String speciesName;
    private Long userId;
    private String userName;
    private String userAvatarUrl;
    private String mediaUrl;
    private String caption;
    private String locationName;
    private String moodTag;
    private String audience;
    private String createdAt;
    private List<PetMomentReactionDto> reactions;
    private Map<String, Long> reactionCounts;
    private boolean isMine;
}

package com.petcare.backend.dto.friend.response;

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
public class FriendshipStatusResponse {
    private Long targetUserId;
    private String relationshipStatus;
    private Long requestId;
}

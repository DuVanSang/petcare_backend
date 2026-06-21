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
public class FriendCountResponse {
    private long friendsCount;
    private long incomingRequestsCount;
    private long outgoingRequestsCount;
}

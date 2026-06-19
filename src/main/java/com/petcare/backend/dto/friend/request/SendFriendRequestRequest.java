package com.petcare.backend.dto.friend.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SendFriendRequestRequest {
    @NotNull(message = "Receiver id is required")
    private Long receiverId;
}

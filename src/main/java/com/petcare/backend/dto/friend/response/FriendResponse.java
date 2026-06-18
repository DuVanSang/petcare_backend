package com.petcare.backend.dto.friend.response;

import java.time.LocalDateTime;
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
public class FriendResponse {
    private Long userId;
    private String fullName;
    private String email;
    private String avatarUrl;
    private String status;
    private LocalDateTime friendsSince;
}

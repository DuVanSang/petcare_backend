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
public class FriendRequestResponse {
    private Long id;
    private Long senderId;
    private String senderName;
    private String senderEmail;
    private String senderAvatarUrl;
    private Long receiverId;
    private String receiverName;
    private String receiverEmail;
    private String receiverAvatarUrl;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
}

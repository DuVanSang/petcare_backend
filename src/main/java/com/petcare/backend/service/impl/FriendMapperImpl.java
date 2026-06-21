package com.petcare.backend.service.impl;

import com.petcare.backend.dto.friend.response.FriendRequestResponse;
import com.petcare.backend.dto.friend.response.FriendResponse;
import com.petcare.backend.dto.friend.response.FriendshipStatusResponse;
import com.petcare.backend.model.FriendRequest;
import com.petcare.backend.model.Friendship;
import com.petcare.backend.model.User;
import com.petcare.backend.service.FriendMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FriendMapperImpl implements FriendMapper {
    @Override
    public FriendRequestResponse toFriendRequestResponse(FriendRequest request) {
        User sender = request.getSender();
        User receiver = request.getReceiver();
        return FriendRequestResponse.builder()
                .id(request.getId())
                .senderId(sender == null ? null : sender.getId())
                .senderName(resolveName(sender))
                .senderEmail(sender == null ? null : sender.getEmail())
                .senderAvatarUrl(sender == null ? null : sender.getAvatarUrl())
                .receiverId(receiver == null ? null : receiver.getId())
                .receiverName(resolveName(receiver))
                .receiverEmail(receiver == null ? null : receiver.getEmail())
                .receiverAvatarUrl(receiver == null ? null : receiver.getAvatarUrl())
                .status(request.getStatus() == null ? null : request.getStatus().getValue())
                .createdAt(request.getCreatedAt())
                .respondedAt(request.getRespondedAt())
                .build();
    }

    @Override
    public FriendResponse toFriendResponse(Friendship friendship, Long currentUserId) {
        User friend = currentUserId != null
                && friendship.getUser1() != null
                && currentUserId.equals(friendship.getUser1().getId())
                ? friendship.getUser2()
                : friendship.getUser1();

        return FriendResponse.builder()
                .userId(friend == null ? null : friend.getId())
                .fullName(resolveName(friend))
                .email(friend == null ? null : friend.getEmail())
                .avatarUrl(friend == null ? null : friend.getAvatarUrl())
                .status(friend == null ? null : friend.getStatus())
                .friendsSince(friendship.getCreatedAt())
                .build();
    }

    @Override
    public FriendshipStatusResponse toFriendshipStatusResponse(
            Long currentUserId,
            Long targetUserId,
            String relationshipStatus,
            Long requestId
    ) {
        return FriendshipStatusResponse.builder()
                .targetUserId(targetUserId)
                .relationshipStatus(relationshipStatus)
                .requestId(requestId)
                .build();
    }

    private String resolveName(User user) {
        if (user == null) {
            return null;
        }
        if (StringUtils.hasText(user.getFullName())) {
            return user.getFullName();
        }
        if (StringUtils.hasText(user.getEmail())) {
            return user.getEmail();
        }
        return "User " + user.getId();
    }
}

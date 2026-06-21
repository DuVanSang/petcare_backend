package com.petcare.backend.service;

import com.petcare.backend.dto.friend.response.FriendRequestResponse;
import com.petcare.backend.dto.friend.response.FriendResponse;
import com.petcare.backend.dto.friend.response.FriendshipStatusResponse;
import com.petcare.backend.model.FriendRequest;
import com.petcare.backend.model.Friendship;

public interface FriendMapper {
    FriendRequestResponse toFriendRequestResponse(FriendRequest request);

    FriendResponse toFriendResponse(Friendship friendship, Long currentUserId);

    FriendshipStatusResponse toFriendshipStatusResponse(
            Long currentUserId,
            Long targetUserId,
            String relationshipStatus,
            Long requestId
    );
}

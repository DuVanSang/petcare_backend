package com.petcare.backend.service;

import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.dto.friend.request.SendFriendRequestRequest;
import com.petcare.backend.dto.friend.response.FriendCountResponse;
import com.petcare.backend.dto.friend.response.FriendRequestResponse;
import com.petcare.backend.dto.friend.response.FriendResponse;
import com.petcare.backend.dto.friend.response.FriendshipStatusResponse;

public interface FriendService {
    FriendRequestResponse sendFriendRequest(Long currentUserId, SendFriendRequestRequest request);

    FriendRequestResponse acceptFriendRequest(Long currentUserId, Long requestId);

    FriendRequestResponse declineFriendRequest(Long currentUserId, Long requestId);

    void cancelFriendRequest(Long currentUserId, Long requestId);

    void unfriend(Long currentUserId, Long friendId);

    PageResponse<FriendRequestResponse> getIncomingRequests(Long currentUserId, int page, int size);

    PageResponse<FriendRequestResponse> getOutgoingRequests(Long currentUserId, int page, int size);

    PageResponse<FriendResponse> getMyFriends(Long currentUserId, int page, int size);

    PageResponse<FriendResponse> getUserFriends(Long currentUserId, Long userId, int page, int size);

    FriendshipStatusResponse getFriendshipStatus(Long currentUserId, Long targetUserId);

    FriendCountResponse getMyFriendCounts(Long currentUserId);

    boolean areFriends(Long userId1, Long userId2);
}

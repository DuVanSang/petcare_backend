package com.petcare.backend.controller;

import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.dto.friend.request.SendFriendRequestRequest;
import com.petcare.backend.dto.friend.response.FriendCountResponse;
import com.petcare.backend.dto.friend.response.FriendRequestResponse;
import com.petcare.backend.dto.friend.response.FriendResponse;
import com.petcare.backend.dto.friend.response.FriendshipStatusResponse;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.FriendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FriendController {
    private final FriendService friendService;

    @PostMapping("/friends/requests")
    public ResponseEntity<ApiResponse<FriendRequestResponse>> sendFriendRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SendFriendRequestRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Friend request sent successfully",
                friendService.sendFriendRequest(principal.getId(), request)
        ));
    }

    @PatchMapping("/friends/requests/{requestId}/accept")
    public ResponseEntity<ApiResponse<FriendRequestResponse>> acceptFriendRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long requestId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Friend request accepted successfully",
                friendService.acceptFriendRequest(principal.getId(), requestId)
        ));
    }

    @PatchMapping("/friends/requests/{requestId}/decline")
    public ResponseEntity<ApiResponse<FriendRequestResponse>> declineFriendRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long requestId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Friend request declined successfully",
                friendService.declineFriendRequest(principal.getId(), requestId)
        ));
    }

    @DeleteMapping("/friends/requests/{requestId}")
    public ResponseEntity<ApiResponse<Void>> cancelFriendRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long requestId
    ) {
        friendService.cancelFriendRequest(principal.getId(), requestId);
        return ResponseEntity.ok(ApiResponse.success("Friend request cancelled successfully", null));
    }

    @DeleteMapping("/friends/{friendId}")
    public ResponseEntity<ApiResponse<Void>> unfriend(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long friendId
    ) {
        friendService.unfriend(principal.getId(), friendId);
        return ResponseEntity.ok(ApiResponse.success("Friend removed successfully", null));
    }

    @GetMapping("/friends/requests/incoming")
    public ResponseEntity<ApiResponse<PageResponse<FriendRequestResponse>>> getIncomingRequests(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Incoming friend requests fetched successfully",
                friendService.getIncomingRequests(principal.getId(), page, size)
        ));
    }

    @GetMapping("/friends/requests/outgoing")
    public ResponseEntity<ApiResponse<PageResponse<FriendRequestResponse>>> getOutgoingRequests(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Outgoing friend requests fetched successfully",
                friendService.getOutgoingRequests(principal.getId(), page, size)
        ));
    }

    @GetMapping("/friends/me")
    public ResponseEntity<ApiResponse<PageResponse<FriendResponse>>> getMyFriends(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Friends fetched successfully",
                friendService.getMyFriends(principal.getId(), page, size)
        ));
    }

    @GetMapping("/users/{userId}/friends")
    public ResponseEntity<ApiResponse<PageResponse<FriendResponse>>> getUserFriends(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "User friends fetched successfully",
                friendService.getUserFriends(principal.getId(), userId, page, size)
        ));
    }

    @GetMapping("/users/{targetUserId}/friendship-status")
    public ResponseEntity<ApiResponse<FriendshipStatusResponse>> getFriendshipStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long targetUserId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Friendship status fetched successfully",
                friendService.getFriendshipStatus(principal.getId(), targetUserId)
        ));
    }

    @GetMapping("/friends/me/counts")
    public ResponseEntity<ApiResponse<FriendCountResponse>> getMyFriendCounts(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Friend counts fetched successfully",
                friendService.getMyFriendCounts(principal.getId())
        ));
    }
}

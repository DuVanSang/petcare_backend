package com.petcare.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.petcare.backend.model.FriendRequest;
import com.petcare.backend.model.Friendship;
import com.petcare.backend.model.User;
import com.petcare.backend.model.enums.FriendRequestStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class FriendMapperImplTest {
    private final FriendMapperImpl mapper = new FriendMapperImpl();

    private User user(Long id, String name, String email, String avatar) {
        User user = new User(); user.setId(id); user.setFullName(name); user.setEmail(email); user.setAvatarUrl(avatar); user.setStatus("active"); return user;
    }

    @Test void friendRequestMapsAllFieldsStatusesAndDoesNotMutateInput() {
        User sender = user(1L, "Alice", "alice@example.com", "avatar-a");
        User receiver = user(2L, "Bob", "bob@example.com", "avatar-b");
        LocalDateTime created = LocalDateTime.of(2025, 1, 1, 10, 0); LocalDateTime responded = created.plusHours(1);
        for (FriendRequestStatus status : FriendRequestStatus.values()) {
            FriendRequest request = new FriendRequest(); request.setId(10L); request.setSender(sender); request.setReceiver(receiver); request.setStatus(status); request.setCreatedAt(created); request.setRespondedAt(responded);
            var response = mapper.toFriendRequestResponse(request);
            assertThat(response.getId()).isEqualTo(10L); assertThat(response.getSenderId()).isEqualTo(1L); assertThat(response.getSenderName()).isEqualTo("Alice");
            assertThat(response.getSenderEmail()).isEqualTo("alice@example.com"); assertThat(response.getSenderAvatarUrl()).isEqualTo("avatar-a");
            assertThat(response.getReceiverId()).isEqualTo(2L); assertThat(response.getReceiverName()).isEqualTo("Bob");
            assertThat(response.getReceiverEmail()).isEqualTo("bob@example.com"); assertThat(response.getReceiverAvatarUrl()).isEqualTo("avatar-b");
            assertThat(response.getStatus()).isEqualTo(status.getValue()); assertThat(response.getCreatedAt()).isEqualTo(created); assertThat(response.getRespondedAt()).isEqualTo(responded);
            assertThat(request.getSender()).isSameAs(sender); assertThat(request.getStatus()).isEqualTo(status);
        }
    }

    @Test void friendRequestMapsNullUsersAndOptionalStatus() {
        FriendRequest request = new FriendRequest(); request.setId(11L); request.setStatus(null);
        var response = mapper.toFriendRequestResponse(request);
        assertThat(response.getSenderId()).isNull(); assertThat(response.getSenderName()).isNull(); assertThat(response.getSenderEmail()).isNull(); assertThat(response.getSenderAvatarUrl()).isNull();
        assertThat(response.getReceiverId()).isNull(); assertThat(response.getReceiverName()).isNull(); assertThat(response.getReceiverEmail()).isNull(); assertThat(response.getReceiverAvatarUrl()).isNull(); assertThat(response.getStatus()).isNull();
    }

    @Test void friendResponseSelectsOtherUserAndResolvesNameFallbacks() {
        User first = user(1L, "Alice", "alice@example.com", "");
        User second = user(2L, "", "bob@example.com", null);
        Friendship friendship = new Friendship(); friendship.setUser1(first); friendship.setUser2(second); friendship.setCreatedAt(LocalDateTime.of(2025, 2, 1, 0, 0));
        var forFirst = mapper.toFriendResponse(friendship, 1L);
        assertThat(forFirst.getUserId()).isEqualTo(2L); assertThat(forFirst.getFullName()).isEqualTo("bob@example.com"); assertThat(forFirst.getEmail()).isEqualTo("bob@example.com"); assertThat(forFirst.getAvatarUrl()).isNull();
        var forSecond = mapper.toFriendResponse(friendship, 2L);
        assertThat(forSecond.getUserId()).isEqualTo(1L); assertThat(forSecond.getFullName()).isEqualTo("Alice"); assertThat(forSecond.getAvatarUrl()).isEmpty();
        var forNullCurrent = mapper.toFriendResponse(friendship, null);
        assertThat(forNullCurrent.getUserId()).isEqualTo(1L); assertThat(forNullCurrent.getFriendsSince()).isEqualTo(friendship.getCreatedAt());
        assertThat(friendship.getUser1()).isSameAs(first); assertThat(friendship.getUser2()).isSameAs(second);
    }

    @Test void friendResponseHandlesMissingFirstUserAndIdFallbackName() {
        Friendship friendship = new Friendship(); friendship.setUser1(null); friendship.setUser2(user(2L, " ", "", "avatar"));
        var withoutFirst = mapper.toFriendResponse(friendship, 1L);
        assertThat(withoutFirst.getUserId()).isNull(); assertThat(withoutFirst.getFullName()).isNull(); assertThat(withoutFirst.getStatus()).isNull();
        friendship.setUser1(user(3L, " ", "", null));
        var fallback = mapper.toFriendResponse(friendship, 2L);
        assertThat(fallback.getFullName()).isEqualTo("User 3");
    }

    @Test void friendshipStatusResponseMapsOptionalFieldsWithoutUsingCurrentUser() {
        var response = mapper.toFriendshipStatusResponse(null, 2L, "pending", 9L);
        assertThat(response.getTargetUserId()).isEqualTo(2L); assertThat(response.getRelationshipStatus()).isEqualTo("pending"); assertThat(response.getRequestId()).isEqualTo(9L);
        var optional = mapper.toFriendshipStatusResponse(1L, null, null, null);
        assertThat(optional.getTargetUserId()).isNull(); assertThat(optional.getRelationshipStatus()).isNull(); assertThat(optional.getRequestId()).isNull();
    }
}

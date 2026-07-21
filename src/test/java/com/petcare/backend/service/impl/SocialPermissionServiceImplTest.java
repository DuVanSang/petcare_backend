package com.petcare.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.petcare.backend.exception.ForbiddenException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.Post;
import com.petcare.backend.model.User;
import com.petcare.backend.model.enums.PostPrivacy;
import com.petcare.backend.model.enums.PostStatus;
import com.petcare.backend.repository.UserRepository;
import com.petcare.backend.service.FriendService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SocialPermissionServiceImplTest {
    @Mock UserRepository users;
    @Mock FriendService friends;
    private SocialPermissionServiceImpl service;

    @BeforeEach void setUp() { service = new SocialPermissionServiceImpl(users, friends); }

    private User user(long id, String status) { User user = new User(); user.setId(id); user.setStatus(status); return user; }
    private Post post(Long ownerId, PostStatus status, PostPrivacy privacy) {
        Post post = new Post(); post.setStatus(status); post.setPrivacy(privacy);
        if (ownerId != null) { User owner = user(ownerId, "active"); post.setUser(owner); }
        return post;
    }
    private void active(long id) { when(users.findById(id)).thenReturn(Optional.of(user(id, "active"))); }

    @Test void checkUserActiveAndCreatePost_coverExistingInactiveAndMissingUsers() {
        active(1L);
        service.checkUserActive(1L); service.checkCanCreatePost(1L);
        when(users.findById(2L)).thenReturn(Optional.of(user(2L, "BANNED")));
        assertThatThrownBy(() -> service.checkUserActive(2L)).isInstanceOf(ForbiddenException.class);
        when(users.findById(3L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.checkCanCreatePost(3L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test void isPostOwner_coversNullOwnerPostAndAllIdPartitions() {
        Post owned = post(1L, PostStatus.PUBLISHED, PostPrivacy.PUBLIC);
        assertThat(service.isPostOwner(null, owned)).isFalse();
        assertThat(service.isPostOwner(0L, null)).isFalse();
        assertThat(service.isPostOwner(-1L, post(null, PostStatus.PUBLISHED, PostPrivacy.PUBLIC))).isFalse();
        assertThat(service.isPostOwner(2L, owned)).isFalse();
        assertThat(service.isPostOwner(1L, owned)).isTrue();
    }

    @Test void canViewPost_coversMissingDeletedOwnerStatusAndPublicPrivacy() {
        assertThat(service.canViewPost(1L, null)).isFalse();
        assertThat(service.canViewPost(1L, post(2L, PostStatus.DELETED, PostPrivacy.PUBLIC))).isFalse();
        assertThat(service.canViewPost(1L, post(1L, PostStatus.HIDDEN, PostPrivacy.PRIVATE))).isTrue();
        assertThat(service.canViewPost(1L, post(2L, PostStatus.HIDDEN, PostPrivacy.PUBLIC))).isFalse();
        assertThat(service.canViewPost(1L, post(2L, PostStatus.PUBLISHED, PostPrivacy.PUBLIC))).isTrue();
        verifyNoInteractions(friends);
    }

    @Test void canViewPost_coversFriendsPrivateNullOwnerAndFriendshipOutcomes() {
        assertThat(service.canViewPost(1L, post(null, PostStatus.PUBLISHED, PostPrivacy.FRIENDS))).isFalse();
        Post friendsOnly = post(2L, PostStatus.PUBLISHED, PostPrivacy.FRIENDS);
        when(friends.areFriends(1L, 2L)).thenReturn(false);
        assertThat(service.canViewPost(1L, friendsOnly)).isFalse();
        when(friends.areFriends(1L, 2L)).thenReturn(true);
        assertThat(service.canViewPost(1L, friendsOnly)).isTrue();
        assertThat(service.canViewPost(1L, post(2L, PostStatus.PUBLISHED, PostPrivacy.PRIVATE))).isFalse();
        verify(friends, times(2)).areFriends(1L, 2L);
    }

    @Test void checkViewUpdateAndDelete_enforceOwnerAndActiveAccount() {
        Post owned = post(1L, PostStatus.PUBLISHED, PostPrivacy.PRIVATE);
        Post other = post(2L, PostStatus.PUBLISHED, PostPrivacy.PRIVATE);
        service.checkCanViewPost(1L, post(2L, PostStatus.PUBLISHED, PostPrivacy.PUBLIC));
        assertThatThrownBy(() -> service.checkCanViewPost(1L, other)).isInstanceOf(ForbiddenException.class);

        active(1L);
        service.checkCanUpdatePost(1L, owned); service.checkCanDeletePost(1L, owned);
        assertThatThrownBy(() -> service.checkCanUpdatePost(1L, other)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.checkCanDeletePost(1L, other)).isInstanceOf(ForbiddenException.class);

        when(users.findById(4L)).thenReturn(Optional.of(user(4L, "inactive")));
        assertThatThrownBy(() -> service.checkCanUpdatePost(4L, post(4L, PostStatus.PUBLISHED, PostPrivacy.PUBLIC)))
                .isInstanceOf(ForbiddenException.class);
    }
}

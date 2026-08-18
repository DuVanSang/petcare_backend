package com.petcare.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.petcare.backend.model.Notification;
import com.petcare.backend.model.Post;
import com.petcare.backend.model.PostComment;
import com.petcare.backend.model.User;
import com.petcare.backend.repository.NotificationRepository;
import com.petcare.backend.service.PushNotificationSender;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SocialNotificationServiceImplTest {
    @Mock NotificationRepository notifications;
    @Mock PushNotificationSender pushNotificationSender;
    private SocialNotificationServiceImpl service;

    @BeforeEach void setUp() { service = new SocialNotificationServiceImpl(notifications, pushNotificationSender); }

    private User user(Long id, String name) { User user = new User(); user.setId(id); user.setFullName(name); return user; }
    private Post post(User owner) { Post post = new Post(); post.setId(10L); post.setUser(owner); return post; }
    private PostComment comment(Long id, User owner, Post post) { PostComment comment = new PostComment(); comment.setId(id); comment.setUser(owner); comment.setPost(post); return comment; }

    @Test void postReaction_earlyReturnsForMissingInputsIdsSelfAndDuplicate() {
        User actor = user(2L, "Actor");
        service.notifyPostReaction(null, actor, "like");
        service.notifyPostReaction(post(null), actor, "like");
        service.notifyPostReaction(post(user(1L, "Owner")), null, "like");
        service.notifyPostReaction(post(user(2L, "Owner")), actor, "like");
        service.notifyPostReaction(post(user(null, "Owner")), actor, "like");
        service.notifyPostReaction(post(user(1L, "Owner")), user(null, "No id"), "like");
        when(notifications.existsByReceiver_IdAndSender_IdAndTypeAndData(1L, 2L, "post_like", "{\"referenceId\":10}"))
                .thenReturn(true);
        service.notifyPostReaction(post(user(1L, "Owner")), actor, "like");
        verify(notifications).existsByReceiver_IdAndSender_IdAndTypeAndData(1L, 2L, "post_like", "{\"referenceId\":10}");
        verify(notifications, never()).save(any());
    }

    @Test void postReaction_createsDistinctLikeAndReactionNotificationsAndFallbackActorNames() {
        Post post = post(user(1L, "Owner"));
        service.notifyPostReaction(post, user(2L, null), "LIKE");
        service.notifyPostReaction(post, user(3L, "   "), "love");
        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notifications, times(2)).save(saved.capture());
        List<Notification> values = saved.getAllValues();
        assertThat(values.get(0).getType()).isEqualTo("post_like");
        assertThat(values.get(1).getType()).isEqualTo("post_reaction");
        assertThat(values).allSatisfy(notification -> {
            assertThat(notification.getBody()).contains("Ai đó");
            assertThat(notification.getData()).isEqualTo("{\"referenceId\":10}");
            assertThat(notification.getStatus()).isEqualTo("sent");
            assertThat(notification.getIsRead()).isFalse();
            assertThat(notification.getSentAt()).isNotNull();
        });
    }

    @Test void postComment_coversEachGuardAndCreatesForDifferentPostOwner() {
        User actor = user(2L, "Alice"); Post validPost = post(user(1L, "Owner")); PostComment validComment = comment(20L, actor, validPost);
        service.notifyPostComment(null, validComment, actor);
        service.notifyPostComment(post(null), validComment, actor);
        service.notifyPostComment(validPost, null, actor);
        service.notifyPostComment(validPost, validComment, null);
        service.notifyPostComment(validPost, validComment, actor);
        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).save(saved.capture());
        assertThat(saved.getValue().getType()).isEqualTo("post_comment");
        assertThat(saved.getValue().getData()).isEqualTo("{\"referenceId\":20}");
        assertThat(saved.getValue().getBody()).contains("Alice");
    }

    @Test void commentReaction_coversEachGuardAndCreatesLikeOrReaction() {
        User actor = user(2L, "Actor"); Post post = post(user(1L, "Post owner")); PostComment valid = comment(30L, user(1L, "Comment owner"), post);
        service.notifyCommentReaction(null, actor, "like");
        service.notifyCommentReaction(comment(30L, null, post), actor, "like");
        service.notifyCommentReaction(comment(30L, user(1L, "Owner"), null), actor, "like");
        service.notifyCommentReaction(valid, null, "like");
        service.notifyCommentReaction(valid, actor, "like");
        service.notifyCommentReaction(valid, user(3L, "Other"), "wow");
        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notifications, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(Notification::getType).containsExactly("comment_like", "comment_reaction");
    }

    @Test void commentReply_coversEachGuardSelfAndSuccessfulReply() {
        User owner = user(1L, "Parent owner"); User actor = user(2L, "Replier"); Post post = post(owner);
        PostComment parent = comment(40L, owner, post); PostComment reply = comment(41L, actor, post);
        service.notifyCommentReply(null, reply, actor);
        service.notifyCommentReply(comment(40L, null, post), reply, actor);
        service.notifyCommentReply(comment(40L, owner, null), reply, actor);
        service.notifyCommentReply(parent, null, actor);
        service.notifyCommentReply(parent, reply, null);
        service.notifyCommentReply(parent, comment(42L, owner, post), owner);
        service.notifyCommentReply(parent, reply, actor);
        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).save(saved.capture());
        assertThat(saved.getValue().getType()).isEqualTo("comment_reply");
        assertThat(saved.getValue().getData()).isEqualTo("{\"referenceId\":41}");
    }
}

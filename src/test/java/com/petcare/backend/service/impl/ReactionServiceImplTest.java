package com.petcare.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.CommentReaction;
import com.petcare.backend.model.Post;
import com.petcare.backend.model.PostComment;
import com.petcare.backend.model.PostReaction;
import com.petcare.backend.model.User;
import com.petcare.backend.model.enums.CommentStatus;
import com.petcare.backend.model.enums.PostStatus;
import com.petcare.backend.model.enums.ReactionType;
import com.petcare.backend.repository.CommentReactionRepository;
import com.petcare.backend.repository.PostCommentRepository;
import com.petcare.backend.repository.PostReactionRepository;
import com.petcare.backend.repository.PostRepository;
import com.petcare.backend.repository.UserRepository;
import com.petcare.backend.service.SocialNotificationService;
import com.petcare.backend.service.SocialPermissionService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReactionServiceImplTest {
    private static final long USER_ID = 7L;
    private static final long POST_ID = 11L;
    private static final long COMMENT_ID = 13L;

    @Mock private PostRepository postRepository;
    @Mock private PostReactionRepository postReactionRepository;
    @Mock private PostCommentRepository postCommentRepository;
    @Mock private CommentReactionRepository commentReactionRepository;
    @Mock private UserRepository userRepository;
    @Mock private SocialPermissionService socialPermissionService;
    @Mock private SocialNotificationService socialNotificationService;
    private ReactionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReactionServiceImpl(postRepository, postReactionRepository, postCommentRepository,
                commentReactionRepository, userRepository, socialPermissionService, socialNotificationService);
    }

    @Test
    void reactToPost_NewValidReaction_CreatesReactionNotifiesAndReturnsSummary() {
        Post post = publishedPost(); User user = user(); stubUser(user); stubPost(post);
        when(postReactionRepository.findByPost_IdAndUser_Id(POST_ID, USER_ID)).thenReturn(Optional.empty());
        when(postReactionRepository.countByPost_Id(POST_ID)).thenReturn(1L);

        var summary = service.reactToPost(POST_ID, " LIKE ", USER_ID);

        assertThat(summary.getTotal()).isEqualTo(1L);
        verify(postReactionRepository).save(org.mockito.ArgumentMatchers.argThat(r -> r.getReactionType() == ReactionType.LIKE
                && r.getPost() == post && r.getUser() == user));
        verify(socialNotificationService).notifyPostReaction(post, user, "like");
    }

    @Test
    void reactToPost_ExistingReaction_ChangesTypeWithoutDuplicateNotification() {
        Post post = publishedPost(); stubUser(user()); stubPost(post);
        PostReaction existing = PostReaction.builder().reactionType(ReactionType.LOVE).build();
        when(postReactionRepository.findByPost_IdAndUser_Id(POST_ID, USER_ID)).thenReturn(Optional.of(existing));

        var summary = service.reactToPost(POST_ID, "haha", USER_ID);

        assertThat(existing.getReactionType()).isEqualTo(ReactionType.HAHA);
        assertThat(summary.getCurrentUserReaction()).isEqualTo("haha");
        verify(postReactionRepository).save(existing);
        verify(socialNotificationService, never()).notifyPostReaction(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void reactToPost_InvalidIdStatusAndReactionPartitions_ThrowBadRequest() {
        stubUser(user());
        assertThatThrownBy(() -> service.reactToPost(null, "like", USER_ID)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.reactToPost(0L, "like", USER_ID)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.reactToPost(-1L, "like", USER_ID)).isInstanceOf(BadRequestException.class);

        Post hidden = publishedPost(); hidden.setStatus(PostStatus.HIDDEN); stubUser(user()); stubPost(hidden);
        assertThatThrownBy(() -> service.reactToPost(POST_ID, "like", USER_ID)).isInstanceOf(BadRequestException.class);

        Post published = publishedPost(); stubPost(published);
        assertThatThrownBy(() -> service.reactToPost(POST_ID, " ", USER_ID)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.reactToPost(POST_ID, "invalid", USER_ID)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void postOperations_MissingPostOrUser_ThrowNotFoundAndRemoveDelegates() {
        when(postRepository.findByIdAndStatusNot(POST_ID, PostStatus.DELETED)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getPostReactionSummary(POST_ID, USER_ID)).isInstanceOf(ResourceNotFoundException.class);

        Post post = publishedPost(); stubPost(post);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.reactToPost(POST_ID, "like", USER_ID)).isInstanceOf(ResourceNotFoundException.class);

        service.removePostReaction(POST_ID, USER_ID);
        verify(postReactionRepository).deleteByPost_IdAndUser_Id(POST_ID, USER_ID);
    }

    @Test
    void getPostReactionSummary_ExistingReactionAndCounts_ReturnsAllPartitions() {
        Post post = publishedPost(); stubPost(post);
        PostReaction reaction = PostReaction.builder().reactionType(ReactionType.CARE).build();
        when(postReactionRepository.findByPost_IdAndUser_Id(POST_ID, USER_ID)).thenReturn(Optional.of(reaction));
        when(postReactionRepository.countByPost_Id(POST_ID)).thenReturn(7L);
        when(postReactionRepository.countByPost_IdAndReactionType(org.mockito.ArgumentMatchers.eq(POST_ID), org.mockito.ArgumentMatchers.any(ReactionType.class))).thenReturn(0L);
        when(postReactionRepository.countByPost_IdAndReactionType(POST_ID, ReactionType.LIKE)).thenReturn(1L);
        when(postReactionRepository.countByPost_IdAndReactionType(POST_ID, ReactionType.CARE)).thenReturn(2L);

        var summary = service.getPostReactionSummary(POST_ID, USER_ID);

        assertThat(summary.getTotal()).isEqualTo(7L);
        assertThat(summary.getLike()).isEqualTo(1L);
        assertThat(summary.getCare()).isEqualTo(2L);
        assertThat(summary.getCurrentUserReaction()).isEqualTo("care");
    }

    @Test
    void reactToComment_NewAndExistingReaction_CreatesOrChangesAndAlwaysNotifies() {
        PostComment comment = visibleComment(); User user = user(); stubUser(user); stubComment(comment);
        when(commentReactionRepository.findByComment_IdAndUser_Id(COMMENT_ID, USER_ID)).thenReturn(Optional.empty());

        service.reactToComment(COMMENT_ID, "love", USER_ID);
        verify(commentReactionRepository).save(org.mockito.ArgumentMatchers.argThat(r -> r.getReactionType() == ReactionType.LOVE
                && r.getComment() == comment && r.getUser() == user));
        verify(socialNotificationService).notifyCommentReaction(comment, user, "love");

        CommentReaction existing = CommentReaction.builder().reactionType(ReactionType.LIKE).build();
        when(commentReactionRepository.findByComment_IdAndUser_Id(COMMENT_ID, USER_ID)).thenReturn(Optional.of(existing));
        service.reactToComment(COMMENT_ID, "wow", USER_ID);
        assertThat(existing.getReactionType()).isEqualTo(ReactionType.WOW);
        verify(commentReactionRepository).save(existing);
        verify(socialNotificationService).notifyCommentReaction(comment, user, "wow");
    }

    @Test
    void commentOperations_VisibilityIdStatusAndTypePartitions_AreHandled() {
        assertThatThrownBy(() -> service.getCommentReactionSummary(null, USER_ID)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.getCommentReactionSummary(0L, USER_ID)).isInstanceOf(BadRequestException.class);
        when(postCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getCommentReactionSummary(COMMENT_ID, USER_ID)).isInstanceOf(ResourceNotFoundException.class);

        PostComment hidden = visibleComment(); hidden.setStatus(CommentStatus.HIDDEN); stubComment(hidden);
        assertThatThrownBy(() -> service.getCommentReactionSummary(COMMENT_ID, USER_ID)).isInstanceOf(ResourceNotFoundException.class);

        PostComment comment = visibleComment(); comment.getPost().setStatus(PostStatus.HIDDEN); stubUser(user()); stubComment(comment);
        assertThatThrownBy(() -> service.reactToComment(COMMENT_ID, "like", USER_ID)).isInstanceOf(BadRequestException.class);
        comment.getPost().setStatus(PostStatus.PUBLISHED);
        assertThatThrownBy(() -> service.reactToComment(COMMENT_ID, "", USER_ID)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.reactToComment(COMMENT_ID, "bad", USER_ID)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void commentSummaryAndRemoval_EmptySummaryAndExistingComment_AreHandled() {
        PostComment comment = visibleComment(); stubComment(comment);
        var emptySummary = service.getCommentReactionSummary(COMMENT_ID, USER_ID);
        assertThat(emptySummary.getTotal()).isZero();
        assertThat(emptySummary.getCurrentUserReaction()).isNull();

        service.removeCommentReaction(COMMENT_ID, USER_ID);
        verify(commentReactionRepository).deleteByComment_IdAndUser_Id(COMMENT_ID, USER_ID);
    }

    private User user() { User user = new User(); user.setId(USER_ID); return user; }
    private Post publishedPost() { return Post.builder().id(POST_ID).status(PostStatus.PUBLISHED).build(); }
    private PostComment visibleComment() { return PostComment.builder().id(COMMENT_ID).post(publishedPost()).status(CommentStatus.VISIBLE).build(); }
    private void stubUser(User user) { when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user)); }
    private void stubPost(Post post) { when(postRepository.findByIdAndStatusNot(POST_ID, PostStatus.DELETED)).thenReturn(Optional.of(post)); }
    private void stubComment(PostComment comment) { when(postCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment)); }
}

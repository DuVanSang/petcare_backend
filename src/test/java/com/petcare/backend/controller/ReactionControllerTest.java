package com.petcare.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petcare.backend.dto.post.request.CommentReactionRequest;
import com.petcare.backend.dto.post.request.PostReactionRequest;
import com.petcare.backend.dto.post.response.ReactionSummaryResponse;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.ReactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReactionControllerTest {
    @Mock private ReactionService reactionService;
    @Mock private UserPrincipal principal;
    private ReactionController controller;

    @BeforeEach
    void setUp() {
        controller = new ReactionController(reactionService);
        when(principal.getId()).thenReturn(7L);
    }

    @Test
    void reactToPost_ValidRequest_DelegatesToService() {
        PostReactionRequest request = new PostReactionRequest(); request.setReactionType("like");
        ReactionSummaryResponse response = mock(ReactionSummaryResponse.class);
        when(reactionService.reactToPost(11L, "like", 7L)).thenReturn(response);

        assertThat(controller.reactToPost(principal, 11L, request).getBody().getData()).isSameAs(response);
        verify(reactionService).reactToPost(11L, "like", 7L);
    }

    @Test
    void removePostReaction_ExistingPost_DelegatesToService() {
        ReactionSummaryResponse response = mock(ReactionSummaryResponse.class);
        when(reactionService.removePostReaction(11L, 7L)).thenReturn(response);

        assertThat(controller.removePostReaction(principal, 11L).getBody().getData()).isSameAs(response);
        verify(reactionService).removePostReaction(11L, 7L);
    }

    @Test
    void getPostReactionSummary_EmptyOrPopulatedSummary_DelegatesToService() {
        ReactionSummaryResponse response = ReactionSummaryResponse.empty();
        when(reactionService.getPostReactionSummary(11L, 7L)).thenReturn(response);

        assertThat(controller.getPostReactionSummary(principal, 11L).getBody().getData()).isSameAs(response);
        verify(reactionService).getPostReactionSummary(11L, 7L);
    }

    @Test
    void reactToComment_ValidRequest_DelegatesToService() {
        CommentReactionRequest request = new CommentReactionRequest(); request.setReactionType("love");
        ReactionSummaryResponse response = mock(ReactionSummaryResponse.class);
        when(reactionService.reactToComment(13L, "love", 7L)).thenReturn(response);

        assertThat(controller.reactToComment(principal, 13L, request).getBody().getData()).isSameAs(response);
        verify(reactionService).reactToComment(13L, "love", 7L);
    }

    @Test
    void removeCommentReaction_ExistingComment_DelegatesToService() {
        ReactionSummaryResponse response = mock(ReactionSummaryResponse.class);
        when(reactionService.removeCommentReaction(13L, 7L)).thenReturn(response);

        assertThat(controller.removeCommentReaction(principal, 13L).getBody().getData()).isSameAs(response);
        verify(reactionService).removeCommentReaction(13L, 7L);
    }

    @Test
    void getCommentReactionSummary_EmptyOrPopulatedSummary_DelegatesToService() {
        ReactionSummaryResponse response = ReactionSummaryResponse.empty();
        when(reactionService.getCommentReactionSummary(13L, 7L)).thenReturn(response);

        assertThat(controller.getCommentReactionSummary(principal, 13L).getBody().getData()).isSameAs(response);
        verify(reactionService).getCommentReactionSummary(13L, 7L);
    }
}

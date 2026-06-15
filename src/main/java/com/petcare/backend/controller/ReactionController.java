package com.petcare.backend.controller;

import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.dto.post.request.CommentReactionRequest;
import com.petcare.backend.dto.post.request.PostReactionRequest;
import com.petcare.backend.dto.post.response.ReactionSummaryResponse;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.ReactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReactionController {
    private final ReactionService reactionService;

    @PutMapping("/posts/{postId}/reaction")
    public ResponseEntity<ApiResponse<ReactionSummaryResponse>> reactToPost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId,
            @Valid @RequestBody PostReactionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Post reaction updated successfully",
                reactionService.reactToPost(postId, request.getReactionType(), principal.getId())
        ));
    }

    @DeleteMapping("/posts/{postId}/reaction")
    public ResponseEntity<ApiResponse<ReactionSummaryResponse>> removePostReaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Post reaction removed successfully",
                reactionService.removePostReaction(postId, principal.getId())
        ));
    }

    @GetMapping("/posts/{postId}/reactions")
    public ResponseEntity<ApiResponse<ReactionSummaryResponse>> getPostReactionSummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Post reactions retrieved successfully",
                reactionService.getPostReactionSummary(postId, principal.getId())
        ));
    }

    @PutMapping("/comments/{commentId}/reaction")
    public ResponseEntity<ApiResponse<ReactionSummaryResponse>> reactToComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentReactionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Comment reaction updated successfully",
                reactionService.reactToComment(commentId, request.getReactionType(), principal.getId())
        ));
    }

    @DeleteMapping("/comments/{commentId}/reaction")
    public ResponseEntity<ApiResponse<ReactionSummaryResponse>> removeCommentReaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long commentId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Comment reaction removed successfully",
                reactionService.removeCommentReaction(commentId, principal.getId())
        ));
    }

    @GetMapping("/comments/{commentId}/reactions")
    public ResponseEntity<ApiResponse<ReactionSummaryResponse>> getCommentReactionSummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long commentId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Comment reactions retrieved successfully",
                reactionService.getCommentReactionSummary(commentId, principal.getId())
        ));
    }
}

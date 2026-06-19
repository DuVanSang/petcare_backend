package com.petcare.backend.controller;

import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.dto.post.response.PostCommentResponse;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.CommentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @PostMapping(value = "/posts/{postId}/comments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostCommentResponse>> createComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId,
            @RequestParam(required = false) String commentText,
            @RequestParam(required = false) Long parentCommentId,
            @RequestParam(required = false) List<MultipartFile> files
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Comment created successfully",
                commentService.createCommentWithFiles(
                        postId,
                        principal.getId(),
                        commentText,
                        parentCommentId,
                        files
                )
        ));
    }

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<PageResponse<PostCommentResponse>>> getPostComments(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Comments retrieved successfully",
                commentService.getPostComments(postId, principal.getId(), page, size)
        ));
    }

    @GetMapping("/comments/{commentId}/replies")
    public ResponseEntity<ApiResponse<PageResponse<PostCommentResponse>>> getCommentReplies(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long commentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Replies retrieved successfully",
                commentService.getCommentReplies(commentId, principal.getId(), page, size)
        ));
    }

    @PutMapping(value = "/comments/{commentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostCommentResponse>> updateComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long commentId,
            @RequestParam(required = false) String commentText,
            @RequestParam(required = false) List<MultipartFile> files
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Comment updated successfully",
                commentService.updateCommentWithFiles(commentId, principal.getId(), commentText, files)
        ));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long commentId
    ) {
        commentService.deleteComment(commentId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Comment deleted successfully", null));
    }
}

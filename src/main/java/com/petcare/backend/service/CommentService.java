package com.petcare.backend.service;

import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.dto.post.response.PostCommentResponse;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface CommentService {
    PostCommentResponse createCommentWithFiles(
            Long postId,
            Long currentUserId,
            String commentText,
            Long parentCommentId,
            List<MultipartFile> files
    );

    PageResponse<PostCommentResponse> getPostComments(Long postId, Long currentUserId, int page, int size);

    PageResponse<PostCommentResponse> getCommentReplies(Long commentId, Long currentUserId, int page, int size);

    PostCommentResponse updateCommentWithFiles(
            Long commentId,
            Long currentUserId,
            String commentText,
            List<MultipartFile> files
    );

    void deleteComment(Long commentId, Long currentUserId);
}

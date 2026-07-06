package com.petcare.backend.repository;

import com.petcare.backend.model.PostComment;
import com.petcare.backend.model.enums.CommentStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostCommentRepository extends JpaRepository<PostComment, Long>, JpaSpecificationExecutor<PostComment> {
    Page<PostComment> findByPost_IdAndParentCommentIdIsNullAndStatusOrderByCreatedAtDesc(
            Long postId,
            CommentStatus status,
            Pageable pageable
    );

    List<PostComment> findByPost_IdAndParentCommentIdIsNullAndStatusOrderByCreatedAtDesc(
            Long postId,
            CommentStatus status
    );

    List<PostComment> findByRootCommentIdAndStatusOrderByCreatedAtAsc(
            Long rootCommentId,
            CommentStatus status
    );

    Page<PostComment> findByParentCommentIdAndStatusOrderByCreatedAtAsc(
            Long parentCommentId,
            CommentStatus status,
            Pageable pageable
    );

    List<PostComment> findByParentCommentIdAndStatusOrderByCreatedAtAsc(
            Long parentCommentId,
            CommentStatus status
    );

    long countByPost_IdAndStatus(Long postId, CommentStatus status);

    long countByParentCommentIdAndStatus(Long parentCommentId, CommentStatus status);

    void deleteByPost_Id(Long postId);
}

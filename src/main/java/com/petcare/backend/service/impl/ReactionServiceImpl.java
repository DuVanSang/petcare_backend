package com.petcare.backend.service.impl;

import com.petcare.backend.dto.post.response.ReactionSummaryResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.CommentReaction;
import com.petcare.backend.model.CommentReactionId;
import com.petcare.backend.model.Post;
import com.petcare.backend.model.PostComment;
import com.petcare.backend.model.PostReaction;
import com.petcare.backend.model.PostReactionId;
import com.petcare.backend.model.User;
import com.petcare.backend.model.enums.CommentStatus;
import com.petcare.backend.model.enums.PostStatus;
import com.petcare.backend.model.enums.ReactionType;
import com.petcare.backend.repository.CommentReactionRepository;
import com.petcare.backend.repository.PostCommentRepository;
import com.petcare.backend.repository.PostReactionRepository;
import com.petcare.backend.repository.PostRepository;
import com.petcare.backend.repository.UserRepository;
import com.petcare.backend.service.ReactionService;
import com.petcare.backend.service.SocialNotificationService;
import com.petcare.backend.service.SocialPermissionService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ReactionServiceImpl implements ReactionService {
    private final PostRepository postRepository;
    private final PostReactionRepository postReactionRepository;
    private final PostCommentRepository postCommentRepository;
    private final CommentReactionRepository commentReactionRepository;
    private final UserRepository userRepository;
    private final SocialPermissionService socialPermissionService;
    private final SocialNotificationService socialNotificationService;

    @Override
    @Transactional
    public ReactionSummaryResponse reactToPost(Long postId, String reactionType, Long currentUserId) {
        socialPermissionService.checkUserActive(currentUserId);
        User user = getUserOrThrow(currentUserId);
        Post post = getPostOrThrow(postId);
        socialPermissionService.checkCanViewPost(currentUserId, post);
        if (!PostStatus.PUBLISHED.equals(post.getStatus())) {
            throw new BadRequestException("Cannot react to hidden or deleted post");
        }

        ReactionType parsedReactionType = parseReactionType(reactionType);
        Optional<PostReaction> existingReaction = postReactionRepository.findByPost_IdAndUser_Id(postId, currentUserId);
        PostReaction reaction = existingReaction
                .orElseGet(() -> PostReaction.builder()
                        .id(new PostReactionId(post.getId(), user.getId()))
                        .post(post)
                        .user(user)
                        .build());
        reaction.setReactionType(parsedReactionType);
        postReactionRepository.save(reaction);
        if (existingReaction.isEmpty()) {
            socialNotificationService.notifyPostReaction(post, user, parsedReactionType.getValue());
        }
        return buildPostReactionSummary(post, currentUserId);
    }

    @Override
    @Transactional
    public ReactionSummaryResponse removePostReaction(Long postId, Long currentUserId) {
        socialPermissionService.checkUserActive(currentUserId);
        Post post = getPostOrThrow(postId);
        socialPermissionService.checkCanViewPost(currentUserId, post);
        postReactionRepository.deleteByPost_IdAndUser_Id(postId, currentUserId);
        return buildPostReactionSummary(post, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public ReactionSummaryResponse getPostReactionSummary(Long postId, Long currentUserId) {
        socialPermissionService.checkUserActive(currentUserId);
        Post post = getPostOrThrow(postId);
        socialPermissionService.checkCanViewPost(currentUserId, post);
        return buildPostReactionSummary(post, currentUserId);
    }

    @Override
    @Transactional
    public ReactionSummaryResponse reactToComment(Long commentId, String reactionType, Long currentUserId) {
        socialPermissionService.checkUserActive(currentUserId);
        User user = getUserOrThrow(currentUserId);
        PostComment comment = getVisibleCommentOrThrow(commentId);
        socialPermissionService.checkCanViewPost(currentUserId, comment.getPost());
        if (!PostStatus.PUBLISHED.equals(comment.getPost().getStatus())) {
            throw new BadRequestException("Cannot react to comments on hidden or deleted post");
        }

        ReactionType parsedReactionType = parseReactionType(reactionType);
        Optional<CommentReaction> existingReaction = commentReactionRepository.findByComment_IdAndUser_Id(
                commentId,
                currentUserId
        );
        CommentReaction reaction = existingReaction
                .orElseGet(() -> CommentReaction.builder()
                        .id(new CommentReactionId(comment.getId(), user.getId()))
                        .comment(comment)
                        .user(user)
                        .build());
        reaction.setReactionType(parsedReactionType);
        commentReactionRepository.save(reaction);
        socialNotificationService.notifyCommentReaction(comment, user, parsedReactionType.getValue());
        return buildCommentReactionSummary(comment, currentUserId);
    }

    @Override
    @Transactional
    public ReactionSummaryResponse removeCommentReaction(Long commentId, Long currentUserId) {
        socialPermissionService.checkUserActive(currentUserId);
        PostComment comment = getVisibleCommentOrThrow(commentId);
        socialPermissionService.checkCanViewPost(currentUserId, comment.getPost());
        commentReactionRepository.deleteByComment_IdAndUser_Id(commentId, currentUserId);
        return buildCommentReactionSummary(comment, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public ReactionSummaryResponse getCommentReactionSummary(Long commentId, Long currentUserId) {
        socialPermissionService.checkUserActive(currentUserId);
        PostComment comment = getVisibleCommentOrThrow(commentId);
        socialPermissionService.checkCanViewPost(currentUserId, comment.getPost());
        return buildCommentReactionSummary(comment, currentUserId);
    }

    private Post getPostOrThrow(Long postId) {
        if (postId == null || postId <= 0) {
            throw new BadRequestException("Post id must be greater than 0");
        }
        return postRepository.findByIdAndStatusNot(postId, PostStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
    }

    private PostComment getVisibleCommentOrThrow(Long commentId) {
        if (commentId == null || commentId <= 0) {
            throw new BadRequestException("Comment id must be greater than 0");
        }
        PostComment comment = postCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        if (!CommentStatus.VISIBLE.equals(comment.getStatus())) {
            throw new ResourceNotFoundException("Comment not found");
        }
        return comment;
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private ReactionType parseReactionType(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException("Reaction type is required");
        }
        try {
            return ReactionType.fromValue(value);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid reaction type");
        }
    }

    private ReactionSummaryResponse buildPostReactionSummary(Post post, Long currentUserId) {
        String currentUserReaction = postReactionRepository.findByPost_IdAndUser_Id(post.getId(), currentUserId)
                .map(reaction -> reaction.getReactionType().getValue())
                .orElse(null);
        return ReactionSummaryResponse.builder()
                .total(postReactionRepository.countByPost_Id(post.getId()))
                .like(postReactionRepository.countByPost_IdAndReactionType(post.getId(), ReactionType.LIKE))
                .love(postReactionRepository.countByPost_IdAndReactionType(post.getId(), ReactionType.LOVE))
                .haha(postReactionRepository.countByPost_IdAndReactionType(post.getId(), ReactionType.HAHA))
                .wow(postReactionRepository.countByPost_IdAndReactionType(post.getId(), ReactionType.WOW))
                .sad(postReactionRepository.countByPost_IdAndReactionType(post.getId(), ReactionType.SAD))
                .angry(postReactionRepository.countByPost_IdAndReactionType(post.getId(), ReactionType.ANGRY))
                .care(postReactionRepository.countByPost_IdAndReactionType(post.getId(), ReactionType.CARE))
                .currentUserReaction(currentUserReaction)
                .build();
    }

    private ReactionSummaryResponse buildCommentReactionSummary(PostComment comment, Long currentUserId) {
        String currentUserReaction = commentReactionRepository
                .findByComment_IdAndUser_Id(comment.getId(), currentUserId)
                .map(reaction -> reaction.getReactionType().getValue())
                .orElse(null);
        return ReactionSummaryResponse.builder()
                .total(commentReactionRepository.countByComment_Id(comment.getId()))
                .like(commentReactionRepository.countByComment_IdAndReactionType(comment.getId(), ReactionType.LIKE))
                .love(commentReactionRepository.countByComment_IdAndReactionType(comment.getId(), ReactionType.LOVE))
                .haha(commentReactionRepository.countByComment_IdAndReactionType(comment.getId(), ReactionType.HAHA))
                .wow(commentReactionRepository.countByComment_IdAndReactionType(comment.getId(), ReactionType.WOW))
                .sad(commentReactionRepository.countByComment_IdAndReactionType(comment.getId(), ReactionType.SAD))
                .angry(commentReactionRepository.countByComment_IdAndReactionType(comment.getId(), ReactionType.ANGRY))
                .care(commentReactionRepository.countByComment_IdAndReactionType(comment.getId(), ReactionType.CARE))
                .currentUserReaction(currentUserReaction)
                .build();
    }
}

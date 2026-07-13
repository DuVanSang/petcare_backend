package com.petcare.backend.service.impl;

import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.dto.post.response.PostCommentResponse;
import com.petcare.backend.dto.post.response.ReactionSummaryResponse;
import com.petcare.backend.dto.upload.UploadFileResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.ConflictException;
import com.petcare.backend.exception.ForbiddenException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.CommentMedia;
import com.petcare.backend.model.Post;
import com.petcare.backend.model.PostComment;
import com.petcare.backend.model.User;
import com.petcare.backend.model.enums.CommentStatus;
import com.petcare.backend.model.enums.MediaType;
import com.petcare.backend.model.enums.PostStatus;
import com.petcare.backend.model.enums.ReactionType;
import com.petcare.backend.repository.CommentMediaRepository;
import com.petcare.backend.repository.CommentReactionRepository;
import com.petcare.backend.repository.PostCommentRepository;
import com.petcare.backend.repository.PostRepository;
import com.petcare.backend.repository.UserRepository;
import com.petcare.backend.service.CommentService;
import com.petcare.backend.service.FileStorageService;
import com.petcare.backend.service.PostMapper;
import com.petcare.backend.service.SocialNotificationService;
import com.petcare.backend.service.SocialPermissionService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_FILES_PER_COMMENT = 10;
    private static final int MAX_REPLY_DEPTH = 2;

    private final PostRepository postRepository;
    private final PostCommentRepository postCommentRepository;
    private final CommentMediaRepository commentMediaRepository;
    private final CommentReactionRepository commentReactionRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final PostMapper postMapper;
    private final SocialPermissionService socialPermissionService;
    private final SocialNotificationService socialNotificationService;

    @Override
    @Transactional
    public PostCommentResponse createCommentWithFiles(
            Long postId,
            Long currentUserId,
            String commentText,
            Long parentCommentId,
            List<MultipartFile> files
    ) {
        socialPermissionService.checkUserActive(currentUserId);
        Post post = getPostOrThrow(postId);
        validatePostAllowsComment(post, currentUserId);
        User user = getUserOrThrow(currentUserId);
        validateCommentInput(commentText, files);

        PostComment comment;
        PostComment parent = null;
        if (parentCommentId == null) {
            comment = createRootComment(post, user, commentText);
        } else {
            parent = getCommentOrThrow(parentCommentId);
            validateParentComment(post, parent);
            comment = createReplyComment(post, user, parent, commentText);
        }

        List<UploadFileResponse> uploadedFiles = fileStorageService.storeCommentMediaFiles(files);
        // TODO: Clean up stored comment files if saving media metadata fails.
        createCommentMedia(comment, uploadedFiles);
        if (parent == null) {
            socialNotificationService.notifyPostComment(post, comment, user);
        } else {
            socialNotificationService.notifyPostComment(post, comment, user);
            if (parent.getUser() == null || post.getUser() == null
                    || !parent.getUser().getId().equals(post.getUser().getId())) {
                socialNotificationService.notifyCommentReply(parent, comment, user);
            }
        }
        return buildCommentResponse(comment, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PostCommentResponse> getPostComments(Long postId, Long currentUserId, int page, int size) {
        socialPermissionService.checkUserActive(currentUserId);
        Post post = getPostOrThrow(postId);
        socialPermissionService.checkCanViewPost(currentUserId, post);
        Pageable pageable = buildPageable(page, size);
        Page<PostComment> comments = postCommentRepository
                .findByPost_IdAndParentCommentIdIsNullAndStatusOrderByCreatedAtDesc(
                        postId,
                        CommentStatus.VISIBLE,
                        pageable
                );
        return toCommentPageResponse(comments, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PostCommentResponse> getCommentReplies(Long commentId, Long currentUserId, int page, int size) {
        socialPermissionService.checkUserActive(currentUserId);
        PostComment comment = getVisibleCommentOrThrow(commentId);
        socialPermissionService.checkCanViewPost(currentUserId, comment.getPost());
        Pageable pageable = buildPageable(page, size);
        Page<PostComment> replies = postCommentRepository.findByParentCommentIdAndStatusOrderByCreatedAtAsc(
                commentId,
                CommentStatus.VISIBLE,
                pageable
        );
        return toCommentPageResponse(replies, currentUserId);
    }

    @Override
    @Transactional
    public PostCommentResponse updateCommentWithFiles(
            Long commentId,
            Long currentUserId,
            String commentText,
            List<MultipartFile> files
    ) {
        socialPermissionService.checkUserActive(currentUserId);
        User currentUser = getUserOrThrow(currentUserId);
        PostComment comment = getVisibleCommentOrThrow(commentId);
        socialPermissionService.checkCanViewPost(currentUserId, comment.getPost());

        if (!isCommentAuthor(currentUserId, comment) && !isAdmin(currentUser)) {
            throw new ForbiddenException("You do not have permission to update this comment");
        }

        validateCommentUpdateInput(commentText, files, comment);
        if (commentText != null) {
            comment.setCommentText(normalizeCommentText(commentText));
        }

        PostComment savedComment = postCommentRepository.save(comment);
        if (files != null) {
            commentMediaRepository.deleteByComment_Id(savedComment.getId());
            List<UploadFileResponse> uploadedFiles = fileStorageService.storeCommentMediaFiles(files);
            // TODO: Clean up stored comment files if saving media metadata fails.
            createCommentMedia(savedComment, uploadedFiles);
        }

        return buildCommentResponse(savedComment, currentUserId);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long currentUserId) {
        socialPermissionService.checkUserActive(currentUserId);
        PostComment comment = getCommentOrThrow(commentId);
        if (CommentStatus.DELETED.equals(comment.getStatus())) {
            return;
        }

        Long authorId = comment.getUser() == null ? null : comment.getUser().getId();
        Long postOwnerId = comment.getPost().getUser() == null ? null : comment.getPost().getUser().getId();
        if (!currentUserId.equals(authorId) && !currentUserId.equals(postOwnerId)) {
            throw new ForbiddenException("You do not have permission to delete this comment");
        }

        List<Long> commentIds = collectVisibleCommentBranchIds(comment);
        List<PostComment> commentsToDelete = postCommentRepository.findAllById(commentIds);
        commentsToDelete.forEach(item -> item.setStatus(CommentStatus.DELETED));
        postCommentRepository.saveAll(commentsToDelete);
        commentReactionRepository.deleteByComment_IdIn(commentIds);
    }

    private Post getPostOrThrow(Long postId) {
        if (postId == null || postId <= 0) {
            throw new BadRequestException("Post id must be greater than 0");
        }
        return postRepository.findByIdAndStatusNot(postId, PostStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
    }

    private PostComment getCommentOrThrow(Long commentId) {
        if (commentId == null || commentId <= 0) {
            throw new BadRequestException("Comment id must be greater than 0");
        }
        return postCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
    }

    private PostComment getVisibleCommentOrThrow(Long commentId) {
        PostComment comment = getCommentOrThrow(commentId);
        if (!CommentStatus.VISIBLE.equals(comment.getStatus())) {
            throw new ResourceNotFoundException("Comment not found");
        }
        return comment;
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Pageable buildPageable(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("Page must not be negative");
        }
        if (size <= 0) {
            throw new BadRequestException("Size must be greater than 0");
        }
        return PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
    }

    private void validatePostAllowsComment(Post post, Long currentUserId) {
        if (PostStatus.HIDDEN.equals(post.getStatus())) {
            throw new ForbiddenException("Cannot comment on hidden post");
        }
        socialPermissionService.checkCanViewPost(currentUserId, post);
        if (Boolean.TRUE.equals(post.getCommentsLocked())) {
            throw new ConflictException("Comments are locked for this post");
        }
    }

    private void validateCommentInput(String commentText, List<MultipartFile> files) {
        validateFiles(files);
        if (!StringUtils.hasText(commentText) && (files == null || files.isEmpty())) {
            throw new BadRequestException("Comment must contain text or media");
        }
    }

    private void validateCommentUpdateInput(String commentText, List<MultipartFile> files, PostComment comment) {
        validateFiles(files);
        String nextText = commentText == null ? comment.getCommentText() : commentText;
        boolean hasMedia = files == null
                ? !commentMediaRepository.findByComment_IdOrderByDisplayOrderAsc(comment.getId()).isEmpty()
                : !files.isEmpty();
        if (!StringUtils.hasText(nextText) && !hasMedia) {
            throw new BadRequestException("Comment must contain text or media");
        }
    }

    private void validateFiles(List<MultipartFile> files) {
        if (files != null && files.size() > MAX_FILES_PER_COMMENT) {
            throw new BadRequestException("A comment can contain at most 10 files");
        }
    }

    private void validateParentComment(Post post, PostComment parent) {
        if (!post.getId().equals(parent.getPost().getId())) {
            throw new BadRequestException("Parent comment does not belong to this post");
        }
        if (!CommentStatus.VISIBLE.equals(parent.getStatus())) {
            throw new BadRequestException("Parent comment is not visible");
        }
        if (parent.getDepth() != null && parent.getDepth() >= MAX_REPLY_DEPTH) {
            throw new BadRequestException("Only two-level replies are supported");
        }
    }

    private PostComment createRootComment(Post post, User user, String commentText) {
        PostComment comment = PostComment.builder()
                .post(post)
                .user(user)
                .parentCommentId(null)
                .rootCommentId(null)
                .depth(0)
                .commentText(normalizeCommentText(commentText))
                .status(CommentStatus.VISIBLE)
                .build();
        PostComment savedComment = postCommentRepository.save(comment);
        savedComment.setRootCommentId(savedComment.getId());
        return postCommentRepository.save(savedComment);
    }

    private PostComment createReplyComment(Post post, User user, PostComment parent, String commentText) {
        PostComment comment = PostComment.builder()
                .post(post)
                .user(user)
                .parentCommentId(parent.getId())
                .rootCommentId(parent.getRootCommentId() == null ? parent.getId() : parent.getRootCommentId())
                .depth((parent.getDepth() == null ? 0 : parent.getDepth()) + 1)
                .commentText(normalizeCommentText(commentText))
                .status(CommentStatus.VISIBLE)
                .build();
        return postCommentRepository.save(comment);
    }

    private List<CommentMedia> createCommentMedia(PostComment comment, List<UploadFileResponse> uploadedFiles) {
        if (uploadedFiles == null || uploadedFiles.isEmpty()) {
            return List.of();
        }

        List<CommentMedia> media = new ArrayList<>();
        for (int i = 0; i < uploadedFiles.size(); i++) {
            UploadFileResponse item = uploadedFiles.get(i);
            media.add(CommentMedia.builder()
                    .comment(comment)
                    .mediaType(MediaType.fromValue(item.getMediaType()))
                    .mediaUrl(item.getMediaUrl())
                    .thumbnailUrl(item.getThumbnailUrl())
                    .originalFilename(item.getOriginalFilename())
                    .mimeType(item.getMimeType())
                    .fileSize(item.getFileSize())
                    .displayOrder(i)
                    .altText(null)
                    .build());
        }
        return commentMediaRepository.saveAll(media);
    }

    private PostCommentResponse buildCommentResponse(PostComment comment, Long currentUserId) {
        return buildCommentResponse(comment, currentUserId, remainingReplyDepth(comment));
    }

    private PostCommentResponse buildCommentResponse(PostComment comment, Long currentUserId, int remainingDepth) {
        List<CommentMedia> media = commentMediaRepository.findByComment_IdOrderByDisplayOrderAsc(comment.getId());
        ReactionSummaryResponse reactions = buildCommentReactionSummary(comment, currentUserId);
        long replyCount = countVisibleReplies(comment.getId());
        Long postOwnerId = comment.getPost().getUser() == null ? null : comment.getPost().getUser().getId();
        PostCommentResponse response = postMapper.toCommentResponse(
                comment,
                media,
                reactions,
                replyCount,
                postOwnerId,
                currentUserId
        );
        response.setReplies(buildReplyResponses(comment.getId(), currentUserId, remainingDepth));
        return response;
    }

    private List<PostCommentResponse> buildReplyResponses(Long commentId, Long currentUserId, int remainingDepth) {
        if (remainingDepth <= 0) {
            return List.of();
        }
        return postCommentRepository.findByParentCommentIdAndStatusOrderByCreatedAtAsc(
                        commentId,
                        CommentStatus.VISIBLE
                )
                .stream()
                .map(reply -> buildCommentResponse(reply, currentUserId, remainingDepth - 1))
                .toList();
    }

    private List<Long> collectVisibleCommentBranchIds(PostComment comment) {
        Set<Long> ids = new LinkedHashSet<>();
        ids.add(comment.getId());

        if (comment.getDepth() == null || comment.getDepth() == 0) {
            postCommentRepository.findByRootCommentIdAndStatusOrderByCreatedAtAsc(
                            comment.getRootCommentId() == null ? comment.getId() : comment.getRootCommentId(),
                            CommentStatus.VISIBLE
                    )
                    .forEach(item -> ids.add(item.getId()));
            return new ArrayList<>(ids);
        }

        postCommentRepository.findByParentCommentIdAndStatusOrderByCreatedAtAsc(
                        comment.getId(),
                        CommentStatus.VISIBLE
                )
                .forEach(item -> ids.add(item.getId()));
        return new ArrayList<>(ids);
    }

    private int remainingReplyDepth(PostComment comment) {
        int depth = comment.getDepth() == null ? 0 : comment.getDepth();
        return Math.max(0, MAX_REPLY_DEPTH - depth);
    }

    private long countVisibleReplies(Long commentId) {
        return postCommentRepository.countByParentCommentIdAndStatus(commentId, CommentStatus.VISIBLE);
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

    private PageResponse<PostCommentResponse> toCommentPageResponse(Page<PostComment> comments, Long currentUserId) {
        List<PostCommentResponse> content = comments.getContent()
                .stream()
                .map(comment -> buildCommentResponse(comment, currentUserId))
                .toList();
        return PageResponse.<PostCommentResponse>builder()
                .content(content)
                .page(comments.getNumber())
                .size(comments.getSize())
                .totalElements(comments.getTotalElements())
                .totalPages(comments.getTotalPages())
                .first(comments.isFirst())
                .last(comments.isLast())
                .build();
    }

    private String normalizeCommentText(String commentText) {
        if (!StringUtils.hasText(commentText)) {
            return "";
        }
        return commentText.trim();
    }

    private boolean isCommentAuthor(Long currentUserId, PostComment comment) {
        return currentUserId != null
                && comment.getUser() != null
                && currentUserId.equals(comment.getUser().getId());
    }

    private boolean isAdmin(User user) {
        return user != null && "admin".equalsIgnoreCase(user.getRole());
    }
}

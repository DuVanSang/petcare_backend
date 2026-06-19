package com.petcare.backend.service.impl;

import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.dto.post.request.CreatePostMediaRequest;
import com.petcare.backend.dto.post.request.CreatePostRequest;
import com.petcare.backend.dto.post.request.UpdatePostRequest;
import com.petcare.backend.dto.post.response.PostCommentResponse;
import com.petcare.backend.dto.post.response.PostResponse;
import com.petcare.backend.dto.post.response.ReactionSummaryResponse;
import com.petcare.backend.dto.upload.UploadFileResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.CommentMedia;
import com.petcare.backend.model.Post;
import com.petcare.backend.model.PostComment;
import com.petcare.backend.model.PostMedia;
import com.petcare.backend.model.User;
import com.petcare.backend.model.enums.CommentStatus;
import com.petcare.backend.model.enums.MediaType;
import com.petcare.backend.model.enums.PostPrivacy;
import com.petcare.backend.model.enums.PostStatus;
import com.petcare.backend.model.enums.ReactionType;
import com.petcare.backend.repository.PetRepository;
import com.petcare.backend.repository.CommentMediaRepository;
import com.petcare.backend.repository.CommentReactionRepository;
import com.petcare.backend.repository.PostCommentRepository;
import com.petcare.backend.repository.PostMediaRepository;
import com.petcare.backend.repository.PostReactionRepository;
import com.petcare.backend.repository.PostRepository;
import com.petcare.backend.repository.UserRepository;
import com.petcare.backend.service.FileStorageService;
import com.petcare.backend.service.FriendService;
import com.petcare.backend.service.PostMapper;
import com.petcare.backend.service.PostService;
import com.petcare.backend.service.SocialPermissionService;
import java.util.ArrayList;
import java.util.List;
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
public class PostServiceImpl implements PostService {
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_MEDIA_PER_POST = 10;
    private static final int MAX_REPLY_DEPTH = 2;

    private final PetRepository petRepository;
    private final PostRepository postRepository;
    private final PostMediaRepository postMediaRepository;
    private final PostReactionRepository postReactionRepository;
    private final PostCommentRepository postCommentRepository;
    private final CommentMediaRepository commentMediaRepository;
    private final CommentReactionRepository commentReactionRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final FriendService friendService;
    private final PostMapper postMapper;
    private final SocialPermissionService socialPermissionService;

    @Override
    @Transactional
    public PostResponse createPost(CreatePostRequest request, Long currentUserId) {
        User currentUser = getCurrentActiveUser(currentUserId);
        validateCreatePostRequest(request);

        Post post = Post.builder()
                .user(currentUser)
                .petId(request.getPetId())
                .caption(trimToNull(request.getCaption()))
                .privacy(parsePostPrivacy(request.getPrivacy()))
                .status(PostStatus.PUBLISHED)
                .commentsLocked(false)
                .build();

        Post savedPost = postRepository.save(post);
        createPostMedia(savedPost, request.getMedia());
        return buildPostResponse(savedPost, currentUserId);
    }

    @Override
    @Transactional
    public PostResponse createPostWithFiles(
            Long currentUserId,
            Long petId,
            String caption,
            String privacy,
            List<MultipartFile> files
    ) {
        User currentUser = getCurrentActiveUser(currentUserId);
        validateCreatePostWithFilesRequest(petId, caption, privacy, files);

        List<UploadFileResponse> uploadedFiles = fileStorageService.storePostMediaFiles(files);
        // TODO: Clean up stored files if saving post/media metadata fails.

        Post post = Post.builder()
                .user(currentUser)
                .petId(petId)
                .caption(trimToNull(caption))
                .privacy(parsePostPrivacy(privacy))
                .status(PostStatus.PUBLISHED)
                .commentsLocked(false)
                .build();

        Post savedPost = postRepository.save(post);
        createPostMediaFromUploads(savedPost, uploadedFiles);
        return buildPostResponse(savedPost, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponse getPostById(Long postId, Long currentUserId) {
        socialPermissionService.checkUserActive(currentUserId);
        Post post = getPostOrThrow(postId);
        socialPermissionService.checkCanViewPost(currentUserId, post);
        return buildPostResponse(post, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getPublicPosts(Long currentUserId, int page, int size) {
        socialPermissionService.checkUserActive(currentUserId);
        Pageable pageable = buildPageable(page, size);
        Page<Post> posts = postRepository.findVisibleFeedPosts(
                currentUserId,
                PostStatus.PUBLISHED,
                PostPrivacy.PUBLIC,
                PostPrivacy.FRIENDS,
                pageable
        );
        return toPostPageResponse(posts, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getMyPosts(Long currentUserId, int page, int size) {
        socialPermissionService.checkUserActive(currentUserId);
        Pageable pageable = buildPageable(page, size);
        Page<Post> posts = postRepository.findByUser_IdAndStatusNotOrderByCreatedAtDesc(
                currentUserId,
                PostStatus.DELETED,
                pageable
        );
        return toPostPageResponse(posts, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getUserPosts(Long profileUserId, Long currentUserId, int page, int size) {
        validatePositiveId(profileUserId, "Profile user id");
        socialPermissionService.checkUserActive(currentUserId);

        if (currentUserId.equals(profileUserId)) {
            return getMyPosts(currentUserId, page, size);
        }

        if (!userRepository.existsById(profileUserId)) {
            throw new ResourceNotFoundException("User not found");
        }

        Pageable pageable = buildPageable(page, size);
        boolean friends = friendService.areFriends(currentUserId, profileUserId);
        List<PostPrivacy> visiblePrivacy = friends
                ? List.of(PostPrivacy.PUBLIC, PostPrivacy.FRIENDS)
                : List.of(PostPrivacy.PUBLIC);
        Page<Post> posts = postRepository.findByUser_IdAndStatusAndPrivacyInOrderByCreatedAtDesc(
                profileUserId,
                PostStatus.PUBLISHED,
                visiblePrivacy,
                pageable
        );
        return toPostPageResponse(posts, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getPetPosts(Long petId, Long currentUserId, int page, int size) {
        validatePositiveId(petId, "Pet id");
        socialPermissionService.checkUserActive(currentUserId);
        // TODO: Check Pet existence, active status, and owner/co-editor permission after Pet module exists.

        Pageable pageable = buildPageable(page, size);
        Page<Post> posts = postRepository.findVisiblePetPosts(
                petId,
                currentUserId,
                PostStatus.DELETED,
                PostStatus.PUBLISHED,
                PostPrivacy.PUBLIC,
                PostPrivacy.FRIENDS,
                pageable
        );
        return toPostPageResponse(posts, currentUserId);
    }

    @Override
    @Transactional
    public PostResponse updatePost(Long postId, UpdatePostRequest request, Long currentUserId) {
        socialPermissionService.checkUserActive(currentUserId);
        Post post = getPostOrThrow(postId);
        socialPermissionService.checkCanUpdatePost(currentUserId, post);
        validateUpdatePostRequest(request, post);

        if (request.getCaption() != null) {
            post.setCaption(trimToNull(request.getCaption()));
        }
        if (request.getPrivacy() != null) {
            post.setPrivacy(parsePostPrivacy(request.getPrivacy()));
        }
        if (request.getCommentsLocked() != null) {
            post.setCommentsLocked(request.getCommentsLocked());
        }
        if (request.isPetIdSet()) {
            validatePetIdIfPresent(request.getPetId());
            // TODO: Check Pet existence, active status, and owner/co-editor permission after Pet module exists.
            post.setPetId(request.getPetId());
        }

        Post savedPost = postRepository.save(post);
        if (request.getMedia() != null) {
            postMediaRepository.deleteByPost_Id(savedPost.getId());
            createPostMedia(savedPost, request.getMedia());
        }

        return buildPostResponse(savedPost, currentUserId);
    }

    @Override
    @Transactional
    public void deletePost(Long postId, Long currentUserId) {
        socialPermissionService.checkUserActive(currentUserId);
        Post post = getPostOrThrow(postId);
        socialPermissionService.checkCanDeletePost(currentUserId, post);
        commentReactionRepository.deleteByComment_Post_Id(post.getId());
        postCommentRepository.deleteByPost_Id(post.getId());
        postReactionRepository.deleteByPost_Id(post.getId());
        postMediaRepository.deleteByPost_Id(post.getId());
        postRepository.delete(post);
    }

    private User getCurrentActiveUser(Long currentUserId) {
        socialPermissionService.checkCanCreatePost(currentUserId);
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Post getPostOrThrow(Long postId) {
        validatePositiveId(postId, "Post id");
        return postRepository.findByIdAndStatusNot(postId, PostStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
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

    private void validateCreatePostRequest(CreatePostRequest request) {
        if (request == null) {
            throw new BadRequestException("Post request is required");
        }
        if (!StringUtils.hasText(request.getPrivacy())) {
            throw new BadRequestException("Privacy is required");
        }
        parsePostPrivacy(request.getPrivacy());
        validatePetIdIfPresent(request.getPetId());
        validateMediaList(request.getMedia());

        if (isBlank(request.getCaption()) && (request.getMedia() == null || request.getMedia().isEmpty())) {
            throw new BadRequestException("Post must contain caption or media");
        }
    }

    private void validateCreatePostWithFilesRequest(
            Long petId,
            String caption,
            String privacy,
            List<MultipartFile> files
    ) {
        if (!StringUtils.hasText(privacy)) {
            throw new BadRequestException("Privacy is required");
        }
        parsePostPrivacy(privacy);
        validatePetIdIfPresent(petId);
        // TODO: Check Pet existence, active status, and owner/co-editor permission after Pet module exists.

        if (files != null && files.size() > MAX_MEDIA_PER_POST) {
            throw new BadRequestException("A post can contain at most 10 media items");
        }

        if (isBlank(caption) && (files == null || files.isEmpty())) {
            throw new BadRequestException("Post must contain caption or media");
        }
    }

    private void validateUpdatePostRequest(UpdatePostRequest request, Post post) {
        if (request == null) {
            throw new BadRequestException("Post request is required");
        }
        if (request.getPrivacy() != null) {
            parsePostPrivacy(request.getPrivacy());
        }
        if (request.isPetIdSet()) {
            validatePetIdIfPresent(request.getPetId());
        }
        validateMediaList(request.getMedia());

        String nextCaption = request.getCaption() == null ? post.getCaption() : request.getCaption();
        boolean hasMedia = request.getMedia() == null
                ? !postMediaRepository.findByPost_IdOrderByDisplayOrderAsc(post.getId()).isEmpty()
                : !request.getMedia().isEmpty();

        if (isBlank(nextCaption) && !hasMedia) {
            throw new BadRequestException("Post must contain caption or media");
        }
    }

    private void validateMediaList(List<CreatePostMediaRequest> media) {
        if (media == null) {
            return;
        }
        if (media.size() > MAX_MEDIA_PER_POST) {
            throw new BadRequestException("A post can contain at most 10 media items");
        }
        for (CreatePostMediaRequest item : media) {
            if (item == null) {
                throw new BadRequestException("Media item is required");
            }
            if (!StringUtils.hasText(item.getMediaUrl())) {
                throw new BadRequestException("Media URL is required");
            }
            parseMediaType(item.getMediaType());
            if (item.getFileSize() != null && item.getFileSize() < 0) {
                throw new BadRequestException("File size must not be negative");
            }
            if (item.getDisplayOrder() != null && item.getDisplayOrder() < 0) {
                throw new BadRequestException("Display order must not be negative");
            }
        }
    }

    private List<PostMedia> createPostMedia(Post post, List<CreatePostMediaRequest> mediaRequests) {
        if (mediaRequests == null || mediaRequests.isEmpty()) {
            return List.of();
        }

        List<PostMedia> media = new ArrayList<>();
        for (int i = 0; i < mediaRequests.size(); i++) {
            CreatePostMediaRequest item = mediaRequests.get(i);
            media.add(PostMedia.builder()
                    .post(post)
                    .mediaType(parseMediaType(item.getMediaType()))
                    .mediaUrl(item.getMediaUrl().trim())
                    .thumbnailUrl(trimToNull(item.getThumbnailUrl()))
                    .originalFilename(trimToNull(item.getOriginalFilename()))
                    .mimeType(trimToNull(item.getMimeType()))
                    .fileSize(item.getFileSize())
                    .displayOrder(item.getDisplayOrder() == null ? i : item.getDisplayOrder())
                    .altText(trimToNull(item.getAltText()))
                    .build());
        }
        return postMediaRepository.saveAll(media);
    }

    private List<PostMedia> createPostMediaFromUploads(Post post, List<UploadFileResponse> uploadedFiles) {
        if (uploadedFiles == null || uploadedFiles.isEmpty()) {
            return List.of();
        }

        List<PostMedia> media = new ArrayList<>();
        for (int i = 0; i < uploadedFiles.size(); i++) {
            UploadFileResponse item = uploadedFiles.get(i);
            media.add(PostMedia.builder()
                    .post(post)
                    .mediaType(parseMediaType(item.getMediaType()))
                    .mediaUrl(item.getMediaUrl())
                    .thumbnailUrl(item.getThumbnailUrl())
                    .originalFilename(item.getOriginalFilename())
                    .mimeType(item.getMimeType())
                    .fileSize(item.getFileSize())
                    .displayOrder(i)
                    .altText(null)
                    .build());
        }
        return postMediaRepository.saveAll(media);
    }

    private PostResponse buildPostResponse(Post post, Long currentUserId) {
        List<PostMedia> media = postMediaRepository.findByPost_IdOrderByDisplayOrderAsc(post.getId());
        ReactionSummaryResponse reactions = buildReactionSummary(post, currentUserId);
        long commentCount = countVisibleComments(post);
        PostResponse response = postMapper.toPostResponse(post, media, reactions, commentCount, currentUserId);
        response.setComments(buildPostCommentResponses(post, currentUserId));
        return response;
    }

    private ReactionSummaryResponse buildReactionSummary(Post post, Long currentUserId) {
        String currentUserReaction = currentUserId == null ? null
                : postReactionRepository.findByPost_IdAndUser_Id(post.getId(), currentUserId)
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

    private long countVisibleComments(Post post) {
        return postCommentRepository.countByPost_IdAndStatus(post.getId(), CommentStatus.VISIBLE);
    }

    private List<PostCommentResponse> buildPostCommentResponses(Post post, Long currentUserId) {
        return postCommentRepository.findByPost_IdAndParentCommentIdIsNullAndStatusOrderByCreatedAtDesc(
                        post.getId(),
                        CommentStatus.VISIBLE
                )
                .stream()
                .map(comment -> buildPostCommentResponse(comment, currentUserId, remainingReplyDepth(comment)))
                .toList();
    }

    private PostCommentResponse buildPostCommentResponse(PostComment comment, Long currentUserId, int remainingDepth) {
        List<CommentMedia> media = commentMediaRepository.findByComment_IdOrderByDisplayOrderAsc(comment.getId());
        ReactionSummaryResponse reactions = buildCommentReactionSummary(comment, currentUserId);
        long replyCount = postCommentRepository.countByParentCommentIdAndStatus(comment.getId(), CommentStatus.VISIBLE);
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
                .map(reply -> buildPostCommentResponse(reply, currentUserId, remainingDepth - 1))
                .toList();
    }

    private ReactionSummaryResponse buildCommentReactionSummary(PostComment comment, Long currentUserId) {
        String currentUserReaction = currentUserId == null ? null
                : commentReactionRepository.findByComment_IdAndUser_Id(comment.getId(), currentUserId)
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

    private int remainingReplyDepth(PostComment comment) {
        int depth = comment.getDepth() == null ? 0 : comment.getDepth();
        return Math.max(0, MAX_REPLY_DEPTH - depth);
    }

    private PageResponse<PostResponse> toPostPageResponse(Page<Post> posts, Long currentUserId) {
        List<PostResponse> content = posts.getContent()
                .stream()
                .map(post -> buildPostResponse(post, currentUserId))
                .toList();

        return PageResponse.<PostResponse>builder()
                .content(content)
                .page(posts.getNumber())
                .size(posts.getSize())
                .totalElements(posts.getTotalElements())
                .totalPages(posts.getTotalPages())
                .first(posts.isFirst())
                .last(posts.isLast())
                .build();
    }

    private PostPrivacy parsePostPrivacy(String value) {
        try {
            return PostPrivacy.fromValue(value);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(
                    "Invalid post privacy. Allowed values: public, friends, private."
            );
        }
    }

    private MediaType parseMediaType(String value) {
        if (value == null) {
            return MediaType.IMAGE;
        }
        try {
            return MediaType.fromValue(value);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid media type");
        }
    }

    private void validatePetIdIfPresent(Long petId) {
        if (petId == null) {
            return;
        }
        if (petId <= 0) {
            throw new BadRequestException("Pet id must be greater than 0");
        }
        if (!petRepository.existsById(petId)) {
            throw new ResourceNotFoundException("Pet not found");
        }
    }

    private void validatePositiveId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new BadRequestException(fieldName + " must be greater than 0");
        }
    }

    private boolean isBlank(String value) {
        return !StringUtils.hasText(value);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}

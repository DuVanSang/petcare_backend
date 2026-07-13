package com.petcare.backend.service.impl;

import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.dto.post.response.PostResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.Post;
import com.petcare.backend.model.PostSave;
import com.petcare.backend.model.User;
import com.petcare.backend.model.enums.PostPrivacy;
import com.petcare.backend.model.enums.PostStatus;
import com.petcare.backend.repository.PostRepository;
import com.petcare.backend.repository.PostSaveRepository;
import com.petcare.backend.repository.UserRepository;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.PostSaveService;
import com.petcare.backend.service.PostService;
import com.petcare.backend.service.SocialPermissionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostSaveServiceImpl implements PostSaveService {
    private static final int MAX_PAGE_SIZE = 50;

    private final PostRepository postRepository;
    private final PostSaveRepository postSaveRepository;
    private final UserRepository userRepository;
    private final SocialPermissionService socialPermissionService;
    private final PostService postService;

    @Override
    @Transactional
    public PostResponse savePost(UserPrincipal principal, Long postId) {
        Long currentUserId = principal.getId();
        socialPermissionService.checkUserActive(currentUserId);
        Post post = getPostOrThrow(postId);
        validateCanSavePost(currentUserId, post);

        if (!postSaveRepository.existsByPost_IdAndUser_Id(postId, currentUserId)) {
            PostSave postSave = new PostSave();
            postSave.setPost(post);
            postSave.setUser(getUser(currentUserId));
            postSaveRepository.save(postSave);
        }
        return postService.buildPostResponse(post, currentUserId);
    }

    @Override
    @Transactional
    public void unsavePost(UserPrincipal principal, Long postId) {
        Long currentUserId = principal.getId();
        socialPermissionService.checkUserActive(currentUserId);
        getPostOrThrow(postId);
        PostSave postSave = postSaveRepository.findByPost_IdAndUser_Id(postId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Saved post not found"));
        postSaveRepository.delete(postSave);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getSavedPosts(UserPrincipal principal, int page, int size) {
        Long currentUserId = principal.getId();
        socialPermissionService.checkUserActive(currentUserId);
        Page<PostSave> savedPosts = postSaveRepository.findVisibleSavedPosts(
                currentUserId,
                PostStatus.PUBLISHED,
                PostPrivacy.PUBLIC,
                PostPrivacy.FRIENDS,
                buildPageable(page, size)
        );
        List<PostResponse> content = savedPosts.getContent().stream()
                .map(saved -> postService.buildPostResponse(saved.getPost(), currentUserId))
                .toList();
        return PageResponse.<PostResponse>builder()
                .content(content)
                .page(savedPosts.getNumber())
                .size(savedPosts.getSize())
                .totalElements(savedPosts.getTotalElements())
                .totalPages(savedPosts.getTotalPages())
                .first(savedPosts.isFirst())
                .last(savedPosts.isLast())
                .build();
    }

    private Post getPostOrThrow(Long postId) {
        if (postId == null || postId <= 0) {
            throw new BadRequestException("Post id must be greater than 0");
        }
        return postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
    }

    private void validateCanSavePost(Long currentUserId, Post post) {
        if (!PostStatus.PUBLISHED.equals(post.getStatus())) {
            throw new BadRequestException("Only published posts can be saved");
        }
        socialPermissionService.checkCanViewPost(currentUserId, post);
    }

    private User getUser(Long userId) {
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
}

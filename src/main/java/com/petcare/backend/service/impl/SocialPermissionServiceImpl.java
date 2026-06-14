package com.petcare.backend.service.impl;

import com.petcare.backend.exception.ForbiddenException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.Post;
import com.petcare.backend.model.User;
import com.petcare.backend.model.enums.PostPrivacy;
import com.petcare.backend.model.enums.PostStatus;
import com.petcare.backend.repository.UserRepository;
import com.petcare.backend.service.SocialPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SocialPermissionServiceImpl implements SocialPermissionService {
    private final UserRepository userRepository;

    @Override
    public void checkUserActive(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!"active".equalsIgnoreCase(user.getStatus())) {
            throw new ForbiddenException("User account is not active");
        }
    }

    @Override
    public void checkCanCreatePost(Long currentUserId) {
        checkUserActive(currentUserId);
    }

    @Override
    public void checkCanViewPost(Long currentUserId, Post post) {
        if (!canViewPost(currentUserId, post)) {
            throw new ForbiddenException("You do not have permission to view this post");
        }
    }

    @Override
    public void checkCanUpdatePost(Long currentUserId, Post post) {
        checkUserActive(currentUserId);
        if (!isPostOwner(currentUserId, post)) {
            throw new ForbiddenException("You do not have permission to update this post");
        }
    }

    @Override
    public void checkCanDeletePost(Long currentUserId, Post post) {
        checkUserActive(currentUserId);
        if (!isPostOwner(currentUserId, post)) {
            throw new ForbiddenException("You do not have permission to delete this post");
        }
    }

    @Override
    public boolean canViewPost(Long currentUserId, Post post) {
        if (post == null || PostStatus.DELETED.equals(post.getStatus())) {
            return false;
        }

        if (isPostOwner(currentUserId, post)) {
            return true;
        }

        if (!PostStatus.PUBLISHED.equals(post.getStatus())) {
            return false;
        }

        if (PostPrivacy.PUBLIC.equals(post.getPrivacy())) {
            return true;
        }

        // TODO: Allow FOLLOWERS posts after Follow module exists.
        return PostPrivacy.FOLLOWERS.equals(post.getPrivacy())
                && isAcceptedFollower(currentUserId, post.getUser().getId());
    }

    @Override
    public boolean isPostOwner(Long currentUserId, Post post) {
        return currentUserId != null
                && post != null
                && post.getUser() != null
                && currentUserId.equals(post.getUser().getId());
    }

    @Override
    public boolean isAcceptedFollower(Long followerId, Long followingId) {
        // TODO: Implement after Follow module exists.
        return false;
    }
}

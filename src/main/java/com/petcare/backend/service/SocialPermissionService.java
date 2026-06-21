package com.petcare.backend.service;

import com.petcare.backend.model.Post;

public interface SocialPermissionService {
    void checkUserActive(Long userId);

    void checkCanCreatePost(Long currentUserId);

    void checkCanViewPost(Long currentUserId, Post post);

    void checkCanUpdatePost(Long currentUserId, Post post);

    void checkCanDeletePost(Long currentUserId, Post post);

    boolean canViewPost(Long currentUserId, Post post);

    boolean isPostOwner(Long currentUserId, Post post);
}

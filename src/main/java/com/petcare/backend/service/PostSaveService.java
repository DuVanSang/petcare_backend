package com.petcare.backend.service;

import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.dto.post.response.PostResponse;
import com.petcare.backend.security.UserPrincipal;

public interface PostSaveService {
    PostResponse savePost(UserPrincipal principal, Long postId);

    void unsavePost(UserPrincipal principal, Long postId);

    PageResponse<PostResponse> getSavedPosts(UserPrincipal principal, int page, int size);
}

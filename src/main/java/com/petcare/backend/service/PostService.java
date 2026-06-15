package com.petcare.backend.service;

import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.dto.post.request.CreatePostRequest;
import com.petcare.backend.dto.post.request.UpdatePostRequest;
import com.petcare.backend.dto.post.response.PostResponse;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface PostService {
    PostResponse createPost(CreatePostRequest request, Long currentUserId);

    PostResponse createPostWithFiles(
            Long currentUserId,
            Long petId,
            String caption,
            String privacy,
            List<MultipartFile> files
    );

    PostResponse getPostById(Long postId, Long currentUserId);

    PageResponse<PostResponse> getPublicPosts(Long currentUserId, int page, int size);

    PageResponse<PostResponse> getMyPosts(Long currentUserId, int page, int size);

    PageResponse<PostResponse> getUserPosts(Long profileUserId, Long currentUserId, int page, int size);

    PageResponse<PostResponse> getPetPosts(Long petId, Long currentUserId, int page, int size);

    PostResponse updatePost(Long postId, UpdatePostRequest request, Long currentUserId);

    void deletePost(Long postId, Long currentUserId);
}

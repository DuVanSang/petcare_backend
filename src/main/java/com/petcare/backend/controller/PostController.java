package com.petcare.backend.controller;

import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.dto.post.request.CreatePostRequest;
import com.petcare.backend.dto.post.request.UpdatePostRequest;
import com.petcare.backend.dto.post.response.PostResponse;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.PostSaveService;
import com.petcare.backend.service.PostService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;
    private final PostSaveService postSaveService;

    @PostMapping(value = "/posts", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreatePostRequest request
    ) {
        PostResponse response = postService.createPost(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Post created successfully", response));
    }

    @PostMapping(value = "/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostResponse>> createPostWithFiles(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false) String caption,
            @RequestParam String privacy,
            @RequestParam(required = false) List<MultipartFile> files
    ) {
        PostResponse response = postService.createPostWithFiles(
                principal.getId(),
                petId,
                caption,
                privacy,
                files
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Post created successfully", response));
    }

    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> getPublicPosts(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Posts retrieved successfully",
                postService.getPublicPosts(principal.getId(), page, size)
        ));
    }

    @GetMapping("/posts/me")
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> getMyPosts(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Posts retrieved successfully",
                postService.getMyPosts(principal.getId(), page, size)
        ));
    }

    @GetMapping("/posts/saved")
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> getSavedPosts(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Saved posts retrieved successfully",
                postSaveService.getSavedPosts(principal, page, size)
        ));
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> getPostById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Post retrieved successfully",
                postService.getPostById(postId, principal.getId())
        ));
    }

    @GetMapping("/users/{userId}/posts")
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> getUserPosts(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Posts retrieved successfully",
                postService.getUserPosts(userId, principal.getId(), page, size)
        ));
    }

    @GetMapping("/pets/{petId}/posts")
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> getPetPosts(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long petId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Posts retrieved successfully",
                postService.getPetPosts(petId, principal.getId(), page, size)
        ));
    }

    @PutMapping(value = "/posts/{postId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId,
            @Valid @RequestBody UpdatePostRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Post updated successfully",
                postService.updatePost(postId, request, principal.getId())
        ));
    }

    @PutMapping(value = "/posts/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostResponse>> updatePostWithFormData(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId,
            @Valid @ModelAttribute UpdatePostRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Post updated successfully",
                postService.updatePost(postId, request, principal.getId())
        ));
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId
    ) {
        postService.deletePost(postId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Post deleted successfully", null));
    }

    @PostMapping("/posts/{postId}/save")
    public ResponseEntity<ApiResponse<PostResponse>> savePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Post saved successfully",
                postSaveService.savePost(principal, postId)
        ));
    }

    @DeleteMapping("/posts/{postId}/save")
    public ResponseEntity<ApiResponse<Void>> unsavePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId
    ) {
        postSaveService.unsavePost(principal, postId);
        return ResponseEntity.ok(ApiResponse.success("Post unsaved successfully", null));
    }
}

package com.petcare.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.dto.post.request.CreatePostRequest;
import com.petcare.backend.dto.post.request.UpdatePostRequest;
import com.petcare.backend.dto.post.response.PostResponse;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.PostSaveService;
import com.petcare.backend.service.PostService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PostControllerTest {
    @Mock private PostService postService; @Mock private PostSaveService postSaveService; @Mock private UserPrincipal principal;
    private PostController controller;
    @BeforeEach void setUp() { controller = new PostController(postService, postSaveService); when(principal.getId()).thenReturn(7L); }
    private PostResponse post() { return mock(PostResponse.class); }
    private PageResponse<PostResponse> page() { return PageResponse.<PostResponse>builder().content(List.of()).build(); }

    @Test void createPost_DelegatesAndReturnsCreated() { CreatePostRequest r = new CreatePostRequest(); PostResponse p=post(); when(postService.createPost(r,7L)).thenReturn(p); assertThat(controller.createPost(principal,r).getStatusCode().value()).isEqualTo(201); verify(postService).createPost(r,7L); }
    @Test void createPostWithFiles_Delegates() { PostResponse p=post(); when(postService.createPostWithFiles(7L,1L,"caption","public",List.of())).thenReturn(p); assertThat(controller.createPostWithFiles(principal,1L,"caption","public",List.of()).getBody().getData()).isSameAs(p); verify(postService).createPostWithFiles(7L,1L,"caption","public",List.of()); }
    @Test void getPublicPosts_Delegates() { var p=page(); when(postService.getPublicPosts(7L,0,10)).thenReturn(p); assertThat(controller.getPublicPosts(principal,0,10).getBody().getData()).isSameAs(p); }
    @Test void getMyPosts_Delegates() { var p=page(); when(postService.getMyPosts(7L,0,10)).thenReturn(p); assertThat(controller.getMyPosts(principal,0,10).getBody().getData()).isSameAs(p); }
    @Test void getSavedPosts_Delegates() { var p=page(); when(postSaveService.getSavedPosts(principal,0,10)).thenReturn(p); assertThat(controller.getSavedPosts(principal,0,10).getBody().getData()).isSameAs(p); }
    @Test void getPostById_Delegates() { PostResponse p=post(); when(postService.getPostById(1L,7L)).thenReturn(p); assertThat(controller.getPostById(principal,1L).getBody().getData()).isSameAs(p); }
    @Test void getUserPosts_Delegates() { var p=page(); when(postService.getUserPosts(3L,7L,0,10)).thenReturn(p); assertThat(controller.getUserPosts(principal,3L,0,10).getBody().getData()).isSameAs(p); }
    @Test void getPetPosts_Delegates() { var p=page(); when(postService.getPetPosts(3L,7L,0,10)).thenReturn(p); assertThat(controller.getPetPosts(principal,3L,0,10).getBody().getData()).isSameAs(p); }
    @Test void updatePost_Delegates() { UpdatePostRequest r=new UpdatePostRequest(); PostResponse p=post(); when(postService.updatePost(1L,r,7L)).thenReturn(p); assertThat(controller.updatePost(principal,1L,r).getBody().getData()).isSameAs(p); assertThat(controller.updatePostWithFormData(principal,1L,r).getBody().getData()).isSameAs(p); }
    @Test void deletePost_Delegates() { assertThat(controller.deletePost(principal,1L).getBody().getData()).isNull(); verify(postService).deletePost(1L,7L); }
    @Test void saveAndUnsavePost_Delegate() { PostResponse p=post(); when(postSaveService.savePost(principal,1L)).thenReturn(p); assertThat(controller.savePost(principal,1L).getBody().getData()).isSameAs(p); assertThat(controller.unsavePost(principal,1L).getBody().getData()).isNull(); verify(postSaveService).unsavePost(principal,1L); }
}

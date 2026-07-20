package com.petcare.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.dto.post.response.PostCommentResponse;
import com.petcare.backend.model.User;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.CommentService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

    @Mock private CommentService commentService;
    private CommentController controller;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        controller = new CommentController(commentService);
        User user = new User();
        user.setId(11L);
        user.setEmail("commenter@example.test");
        user.setPasswordHash("hash");
        user.setRole("user");
        user.setStatus("active");
        principal = UserPrincipal.from(user);
    }

    // EP: valid text-only root comment.
    @Test
    void createComment_WithText_ReturnsCreatedResponse() {
        PostCommentResponse expected = PostCommentResponse.builder().id(31L).commentText("hello").build();
        when(commentService.createCommentWithFiles(7L, 11L, "hello", null, null)).thenReturn(expected);

        ResponseEntity<ApiResponse<PostCommentResponse>> response =
                controller.createComment(principal, 7L, "hello", null, null);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody().getMessage()).isEqualTo("Comment created successfully");
        assertThat(response.getBody().getData()).isSameAs(expected);
        verify(commentService).createCommentWithFiles(7L, 11L, "hello", null, null);
    }

    // EP: valid reply with a media file.
    @Test
    void createComment_WithParentAndMedia_DelegatesAllRequestValues() {
        List files = List.of(new MockMultipartFile("files", "cat.jpg", "image/jpeg", new byte[] {1}));
        PostCommentResponse expected = PostCommentResponse.builder().id(32L).build();
        when(commentService.createCommentWithFiles(7L, 11L, null, 20L, files)).thenReturn(expected);

        ResponseEntity<ApiResponse<PostCommentResponse>> response =
                controller.createComment(principal, 7L, null, 20L, files);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        verify(commentService).createCommentWithFiles(7L, 11L, null, 20L, files);
    }

    // BVA: controller defaults are page=0 and size=20.
    @Test
    void getPostComments_WithDefaultPaging_ReturnsServicePage() {
        PageResponse<PostCommentResponse> expected = page(0, 20);
        when(commentService.getPostComments(7L, 11L, 0, 20)).thenReturn(expected);

        ResponseEntity<ApiResponse<PageResponse<PostCommentResponse>>> response =
                controller.getPostComments(principal, 7L, 0, 20);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getMessage()).isEqualTo("Comments retrieved successfully");
        assertThat(response.getBody().getData()).isSameAs(expected);
        verify(commentService).getPostComments(7L, 11L, 0, 20);
    }

    // BVA: valid explicit lower size boundary is passed through unchanged.
    @Test
    void getCommentReplies_WithSizeOne_ReturnsReplies() {
        PageResponse<PostCommentResponse> expected = page(0, 1);
        when(commentService.getCommentReplies(20L, 11L, 0, 1)).thenReturn(expected);

        ResponseEntity<ApiResponse<PageResponse<PostCommentResponse>>> response =
                controller.getCommentReplies(principal, 20L, 0, 1);

        assertThat(response.getBody().getMessage()).isEqualTo("Replies retrieved successfully");
        assertThat(response.getBody().getData().getSize()).isEqualTo(1);
        verify(commentService).getCommentReplies(20L, 11L, 0, 1);
    }

    // EP: update with replacement media is delegated without controller-side mutation.
    @Test
    void updateComment_WithTextAndFiles_ReturnsUpdatedResponse() {
        List files = List.of(new MockMultipartFile("files", "cat.jpg", "image/jpeg", new byte[] {1}));
        PostCommentResponse expected = PostCommentResponse.builder().id(20L).commentText("updated").build();
        when(commentService.updateCommentWithFiles(20L, 11L, "updated", files)).thenReturn(expected);

        ResponseEntity<ApiResponse<PostCommentResponse>> response =
                controller.updateComment(principal, 20L, "updated", files);

        assertThat(response.getBody().getMessage()).isEqualTo("Comment updated successfully");
        assertThat(response.getBody().getData()).isSameAs(expected);
        verify(commentService).updateCommentWithFiles(20L, 11L, "updated", files);
    }

    // EP: valid delete request returns successful empty payload.
    @Test
    void deleteComment_WithValidId_ReturnsSuccess() {
        ResponseEntity<ApiResponse<Void>> response = controller.deleteComment(principal, 20L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getMessage()).isEqualTo("Comment deleted successfully");
        assertThat(response.getBody().getData()).isNull();
        verify(commentService).deleteComment(20L, 11L);
    }

    private PageResponse<PostCommentResponse> page(int page, int size) {
        return PageResponse.<PostCommentResponse>builder()
                .content(List.of(PostCommentResponse.builder().id(20L).build()))
                .page(page).size(size).totalElements(1).totalPages(1).first(page == 0).last(true).build();
    }
}

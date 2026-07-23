package com.petcare.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.dto.pet.response.PetAlbumMediaResponse;
import com.petcare.backend.model.User;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.PetAlbumService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class PetAlbumControllerTest {

    @Mock
    private PetAlbumService petAlbumService;

    private MockMvc mockMvc;
    private PetAlbumController controller;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        controller = new PetAlbumController(petAlbumService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        principal = UserPrincipal.from(activeUser(11L));
    }

    // EP: petId, page, size all belong to valid partitions.
    @Test
    void getPetAlbum_WithValidExplicitPaging_ReturnsAlbumResponse() throws Exception {
        PageResponse<PetAlbumMediaResponse> album = pageResponse(1, 30);
        when(petAlbumService.getPetAlbumImages(11L, 7L, 1, 30)).thenReturn(album);

        ResponseEntity<ApiResponse<PageResponse<PetAlbumMediaResponse>>> response =
                controller.getPetAlbum(principal, 7L, 1, 30);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).isEqualTo("Pet album fetched successfully");
        assertThat(response.getBody().getData()).isSameAs(album);
        assertThat(response.getBody().getData().getContent()).hasSize(1);
        assertThat(response.getBody().getData().getContent().get(0).getMediaId()).isEqualTo(101L);
        assertThat(response.getBody().getData().getPage()).isEqualTo(1);
        assertThat(response.getBody().getData().getSize()).isEqualTo(30);

        verify(petAlbumService).getPetAlbumImages(11L, 7L, 1, 30);
    }

    // BVA: omitted page and size use lower/default controller boundaries page=0, size=30.
    @Test
    void getPetAlbum_WithoutPagingParams_UsesDefaultPageAndSize() throws Exception {
        PageResponse<PetAlbumMediaResponse> album = pageResponse(0, 30);
        when(petAlbumService.getPetAlbumImages(11L, 7L, 0, 30)).thenReturn(album);

        ResponseEntity<ApiResponse<PageResponse<PetAlbumMediaResponse>>> response =
                controller.getPetAlbum(principal, 7L, 0, 30);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getPage()).isEqualTo(0);
        assertThat(response.getBody().getData().getSize()).isEqualTo(30);

        verify(petAlbumService).getPetAlbumImages(11L, 7L, 0, 30);
    }

    // BVA: size=1 is the valid lower boundary and should be passed to the service.
    @Test
    void getPetAlbum_WithSizeOne_PassesBoundaryToService() throws Exception {
        PageResponse<PetAlbumMediaResponse> album = pageResponse(0, 1);
        when(petAlbumService.getPetAlbumImages(11L, 7L, 0, 1)).thenReturn(album);

        ResponseEntity<ApiResponse<PageResponse<PetAlbumMediaResponse>>> response =
                controller.getPetAlbum(principal, 7L, 0, 1);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getSize()).isEqualTo(1);
        verify(petAlbumService).getPetAlbumImages(11L, 7L, 0, 1);
    }

    // EP: non-numeric page belongs to the invalid request parameter partition.
    @Test
    void getPetAlbum_WithNonNumericPage_ReturnsBadRequestBeforeCallingService() throws Exception {
        mockMvc.perform(get("/api/v1/pets/{petId}/album", 7L)
                        .param("page", "abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(petAlbumService);
    }

    // EP: non-numeric petId belongs to the invalid path variable partition.
    @Test
    void getPetAlbum_WithNonNumericPetId_ReturnsBadRequestBeforeCallingService() throws Exception {
        mockMvc.perform(get("/api/v1/pets/not-a-number/album"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(petAlbumService);
    }

    private PageResponse<PetAlbumMediaResponse> pageResponse(int page, int size) {
        PetAlbumMediaResponse media = PetAlbumMediaResponse.builder()
                .mediaId(101L)
                .postId(201L)
                .petId(7L)
                .mediaUrl("https://example.test/pet.jpg")
                .thumbnailUrl("https://example.test/pet-thumb.jpg")
                .mediaType("image")
                .originalFilename("pet.jpg")
                .mimeType("image/jpeg")
                .fileSize(2048L)
                .displayOrder(1)
                .postCaption("A sunny walk")
                .postPrivacy("public")
                .postCreatedAt(LocalDateTime.of(2026, 7, 13, 9, 30))
                .postOwnerId(11L)
                .postOwnerName("Album Owner")
                .postOwnerAvatarUrl("https://example.test/avatar.jpg")
                .build();

        return PageResponse.<PetAlbumMediaResponse>builder()
                .content(List.of(media))
                .page(page)
                .size(size)
                .totalElements(1)
                .totalPages(1)
                .first(page == 0)
                .last(true)
                .build();
    }

    private User activeUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("album-owner@example.test");
        user.setPasswordHash("encoded");
        user.setStatus("active");
        user.setRole("user");
        return user;
    }
}

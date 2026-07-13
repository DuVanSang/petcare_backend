package com.petcare.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.dto.pet.response.PetAlbumMediaResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.Post;
import com.petcare.backend.model.PostMedia;
import com.petcare.backend.model.User;
import com.petcare.backend.model.enums.MediaType;
import com.petcare.backend.model.enums.PostPrivacy;
import com.petcare.backend.model.enums.PostStatus;
import com.petcare.backend.repository.PetRepository;
import com.petcare.backend.repository.PostMediaRepository;
import com.petcare.backend.service.SocialPermissionService;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class PetAlbumServiceImplTest {

    @Mock
    private PetRepository petRepository;

    @Mock
    private PostMediaRepository postMediaRepository;

    @Mock
    private SocialPermissionService socialPermissionService;

    private PetAlbumServiceImpl service;

    private User mockUser;
    private Post mockPost;
    private PostMedia mockMedia;
    private Page<PostMedia> mockPostMediaPage;

    @BeforeEach
    void setUp() {
        service = new PetAlbumServiceImpl(petRepository, postMediaRepository, socialPermissionService);

        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@example.com");
        mockUser.setFullName("Test User");
        mockUser.setAvatarUrl("http://example.com/avatar.jpg");

        mockPost = new Post();
        mockPost.setId(1L);
        mockPost.setPetId(1L);
        mockPost.setUser(mockUser);
        mockPost.setCaption("Test caption");
        mockPost.setPrivacy(PostPrivacy.PUBLIC);
        mockPost.setStatus(PostStatus.PUBLISHED);
        mockPost.setCreatedAt(LocalDateTime.now());

        mockMedia = new PostMedia();
        mockMedia.setId(1L);
        mockMedia.setPost(mockPost);
        mockMedia.setMediaUrl("http://example.com/image.jpg");
        mockMedia.setThumbnailUrl("http://example.com/thumb.jpg");
        mockMedia.setMediaType(MediaType.IMAGE);
        mockMedia.setOriginalFilename("image.jpg");
        mockMedia.setMimeType("image/jpeg");
        mockMedia.setFileSize(1024L);
        mockMedia.setDisplayOrder(1);

        mockPostMediaPage = new PageImpl<>(Collections.singletonList(mockMedia), PageRequest.of(0, 30), 1);
    }

    // EP: currentUserId hợp lệ, petId hợp lệ, page hợp lệ, size hợp lệ
    @Test
    void getPetAlbumImages_WithValidParameters_ReturnsPageResponse() {
        doNothing().when(socialPermissionService).checkUserActive(1L);
        when(petRepository.existsById(1L)).thenReturn(true);
        when(postMediaRepository.findVisiblePetAlbumImages(
                eq(1L), eq(1L), eq(PostStatus.PUBLISHED), eq(MediaType.IMAGE),
                eq(PostPrivacy.PUBLIC), eq(PostPrivacy.FRIENDS), any(Pageable.class)))
                .thenReturn(mockPostMediaPage);

        PageResponse<PetAlbumMediaResponse> result = service.getPetAlbumImages(1L, 1L, 0, 30);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getMediaId()).isEqualTo(1L);
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(30);
    }

    // BVA: page = 0 (giá trị biên dưới hợp lệ)
    @Test
    void getPetAlbumImages_WithPageZero_ReturnsPageResponse() {
        doNothing().when(socialPermissionService).checkUserActive(1L);
        when(petRepository.existsById(1L)).thenReturn(true);
        when(postMediaRepository.findVisiblePetAlbumImages(
                eq(1L), eq(1L), eq(PostStatus.PUBLISHED), eq(MediaType.IMAGE),
                eq(PostPrivacy.PUBLIC), eq(PostPrivacy.FRIENDS), any(Pageable.class)))
                .thenReturn(mockPostMediaPage);

        PageResponse<PetAlbumMediaResponse> result = service.getPetAlbumImages(1L, 1L, 0, 30);

        assertThat(result).isNotNull();
        assertThat(result.getPage()).isEqualTo(0);
    }

    // BVA: page = 1 (giá trị biên trên hợp lệ)
    @Test
    void getPetAlbumImages_WithPageOne_ReturnsPageResponse() {
        doNothing().when(socialPermissionService).checkUserActive(1L);
        when(petRepository.existsById(1L)).thenReturn(true);
        Page<PostMedia> pageOnePage = new PageImpl<>(Collections.singletonList(mockMedia), PageRequest.of(1, 30), 1);
        when(postMediaRepository.findVisiblePetAlbumImages(
                eq(1L), eq(1L), eq(PostStatus.PUBLISHED), eq(MediaType.IMAGE),
                eq(PostPrivacy.PUBLIC), eq(PostPrivacy.FRIENDS), any(Pageable.class)))
                .thenReturn(pageOnePage);

        PageResponse<PetAlbumMediaResponse> result = service.getPetAlbumImages(1L, 1L, 1, 30);

        assertThat(result).isNotNull();
        assertThat(result.getPage()).isEqualTo(1);
    }

    // BVA: size = 1 (giá trị biên dưới hợp lệ)
    @Test
    void getPetAlbumImages_WithSizeOne_ReturnsPageResponse() {
        doNothing().when(socialPermissionService).checkUserActive(1L);
        when(petRepository.existsById(1L)).thenReturn(true);
        Page<PostMedia> sizeOnePage = new PageImpl<>(Collections.singletonList(mockMedia), PageRequest.of(0, 1), 1);
        when(postMediaRepository.findVisiblePetAlbumImages(
                eq(1L), eq(1L), eq(PostStatus.PUBLISHED), eq(MediaType.IMAGE),
                eq(PostPrivacy.PUBLIC), eq(PostPrivacy.FRIENDS), any(Pageable.class)))
                .thenReturn(sizeOnePage);

        PageResponse<PetAlbumMediaResponse> result = service.getPetAlbumImages(1L, 1L, 0, 1);

        assertThat(result).isNotNull();
        assertThat(result.getSize()).isEqualTo(1);
    }

    // BVA: size = 50 (giá trị biên trên hợp lệ - MAX_PAGE_SIZE)
    @Test
    void getPetAlbumImages_WithSizeMax_ReturnsPageResponse() {
        doNothing().when(socialPermissionService).checkUserActive(1L);
        when(petRepository.existsById(1L)).thenReturn(true);
        Page<PostMedia> sizeMaxPage = new PageImpl<>(Collections.singletonList(mockMedia), PageRequest.of(0, 50), 1);
        when(postMediaRepository.findVisiblePetAlbumImages(
                eq(1L), eq(1L), eq(PostStatus.PUBLISHED), eq(MediaType.IMAGE),
                eq(PostPrivacy.PUBLIC), eq(PostPrivacy.FRIENDS), any(Pageable.class)))
                .thenReturn(sizeMaxPage);

        PageResponse<PetAlbumMediaResponse> result = service.getPetAlbumImages(1L, 1L, 0, 50);

        assertThat(result).isNotNull();
        assertThat(result.getSize()).isEqualTo(50);
    }

    // BVA: size = 51 (giá trị biên trên không hợp lệ - vượt MAX_PAGE_SIZE, nhưng sẽ được trim xuống 50)
    @Test
    void getPetAlbumImages_WithSizeOverMax_TrimsToMax() {
        doNothing().when(socialPermissionService).checkUserActive(1L);
        when(petRepository.existsById(1L)).thenReturn(true);
        Page<PostMedia> sizeMaxPage = new PageImpl<>(Collections.singletonList(mockMedia), PageRequest.of(0, 50), 1);
        when(postMediaRepository.findVisiblePetAlbumImages(
                eq(1L), eq(1L), eq(PostStatus.PUBLISHED), eq(MediaType.IMAGE),
                eq(PostPrivacy.PUBLIC), eq(PostPrivacy.FRIENDS), any(Pageable.class)))
                .thenReturn(sizeMaxPage);

        PageResponse<PetAlbumMediaResponse> result = service.getPetAlbumImages(1L, 1L, 0, 51);

        assertThat(result).isNotNull();
        assertThat(result.getSize()).isEqualTo(50); // Should be trimmed to MAX_PAGE_SIZE
    }

    // BVA: page = -1 (giá trị biên dưới không hợp lệ)
    @Test
    void getPetAlbumImages_WithNegativePage_ThrowsBadRequestException() {
        doNothing().when(socialPermissionService).checkUserActive(1L);
        when(petRepository.existsById(1L)).thenReturn(true);
        
        assertThatThrownBy(() -> service.getPetAlbumImages(1L, 1L, -1, 30))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Page must not be negative");
    }

    // BVA: size = 0 (giá trị biên dưới không hợp lệ)
    @Test
    void getPetAlbumImages_WithSizeZero_ThrowsBadRequestException() {
        doNothing().when(socialPermissionService).checkUserActive(1L);
        when(petRepository.existsById(1L)).thenReturn(true);
        
        assertThatThrownBy(() -> service.getPetAlbumImages(1L, 1L, 0, 0))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Size must be greater than 0");
    }

    // BVA: size = -1 (giá trị biên dưới không hợp lệ)
    @Test
    void getPetAlbumImages_WithNegativeSize_ThrowsBadRequestException() {
        doNothing().when(socialPermissionService).checkUserActive(1L);
        when(petRepository.existsById(1L)).thenReturn(true);
        
        assertThatThrownBy(() -> service.getPetAlbumImages(1L, 1L, 0, -1))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Size must be greater than 0");
    }

    // EP: petId = null (không hợp lệ)
    @Test
    void getPetAlbumImages_WithNullPetId_ThrowsBadRequestException() {
        assertThatThrownBy(() -> service.getPetAlbumImages(1L, null, 0, 30))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Pet id must be greater than 0");
    }

    // BVA: petId = 0 (giá trị biên không hợp lệ)
    @Test
    void getPetAlbumImages_WithPetIdZero_ThrowsBadRequestException() {
        assertThatThrownBy(() -> service.getPetAlbumImages(1L, 0L, 0, 30))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Pet id must be greater than 0");
    }

    // BVA: petId = -1 (giá trị biên không hợp lệ)
    @Test
    void getPetAlbumImages_WithNegativePetId_ThrowsBadRequestException() {
        assertThatThrownBy(() -> service.getPetAlbumImages(1L, -1L, 0, 30))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Pet id must be greater than 0");
    }

    // BVA: petId = 1 (giá trị biên dưới hợp lệ)
    @Test
    void getPetAlbumImages_WithPetIdOne_ReturnsPageResponse() {
        doNothing().when(socialPermissionService).checkUserActive(1L);
        when(petRepository.existsById(1L)).thenReturn(true);
        when(postMediaRepository.findVisiblePetAlbumImages(
                eq(1L), eq(1L), eq(PostStatus.PUBLISHED), eq(MediaType.IMAGE),
                eq(PostPrivacy.PUBLIC), eq(PostPrivacy.FRIENDS), any(Pageable.class)))
                .thenReturn(mockPostMediaPage);

        PageResponse<PetAlbumMediaResponse> result = service.getPetAlbumImages(1L, 1L, 0, 30);

        assertThat(result).isNotNull();
    }

    // EP: petId không tồn tại
    @Test
    void getPetAlbumImages_WithNonExistentPetId_ThrowsResourceNotFoundException() {
        doNothing().when(socialPermissionService).checkUserActive(1L);
        when(petRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.getPetAlbumImages(1L, 999L, 0, 30))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Pet not found");
    }

    // EP: Kết quả rỗng
    @Test
    void getPetAlbumImages_WithEmptyResult_ReturnsEmptyPageResponse() {
        Page<PostMedia> emptyPage = new PageImpl<>(
                Collections.emptyList(),
                PageRequest.of(0, 30),
                0
        );

        doNothing().when(socialPermissionService).checkUserActive(1L);
        when(petRepository.existsById(1L)).thenReturn(true);
        when(postMediaRepository.findVisiblePetAlbumImages(
                eq(1L), eq(1L), eq(PostStatus.PUBLISHED), eq(MediaType.IMAGE),
                eq(PostPrivacy.PUBLIC), eq(PostPrivacy.FRIENDS), any(Pageable.class)))
                .thenReturn(emptyPage);

        PageResponse<PetAlbumMediaResponse> result = service.getPetAlbumImages(1L, 1L, 0, 30);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    // EP: petId lớn
    @Test
    void getPetAlbumImages_WithLargePetId_ReturnsPageResponse() {
        doNothing().when(socialPermissionService).checkUserActive(1L);
        when(petRepository.existsById(999999L)).thenReturn(true);
        when(postMediaRepository.findVisiblePetAlbumImages(
                eq(999999L), eq(1L), eq(PostStatus.PUBLISHED), eq(MediaType.IMAGE),
                eq(PostPrivacy.PUBLIC), eq(PostPrivacy.FRIENDS), any(Pageable.class)))
                .thenReturn(mockPostMediaPage);

        PageResponse<PetAlbumMediaResponse> result = service.getPetAlbumImages(1L, 999999L, 0, 30);

        assertThat(result).isNotNull();
    }

    // EP: page lớn
    @Test
    void getPetAlbumImages_WithLargePage_ReturnsPageResponse() {
        doNothing().when(socialPermissionService).checkUserActive(1L);
        when(petRepository.existsById(1L)).thenReturn(true);
        Page<PostMedia> largePage = new PageImpl<>(Collections.singletonList(mockMedia), PageRequest.of(1000, 30), 1);
        when(postMediaRepository.findVisiblePetAlbumImages(
                eq(1L), eq(1L), eq(PostStatus.PUBLISHED), eq(MediaType.IMAGE),
                eq(PostPrivacy.PUBLIC), eq(PostPrivacy.FRIENDS), any(Pageable.class)))
                .thenReturn(largePage);

        PageResponse<PetAlbumMediaResponse> result = service.getPetAlbumImages(1L, 1L, 1000, 30);

        assertThat(result).isNotNull();
        assertThat(result.getPage()).isEqualTo(1000);
    }

    // EP: currentUserId null
    @Test
    void getPetAlbumImages_WithNullCurrentUserId_ThrowsException() {
        assertThatThrownBy(() -> service.getPetAlbumImages(null, 1L, 0, 30))
                .isInstanceOf(Exception.class);
    }
}

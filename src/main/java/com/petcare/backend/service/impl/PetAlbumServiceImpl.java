package com.petcare.backend.service.impl;

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
import com.petcare.backend.service.PetAlbumService;
import com.petcare.backend.service.SocialPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PetAlbumServiceImpl implements PetAlbumService {
    private static final int MAX_PAGE_SIZE = 50;

    private final PetRepository petRepository;
    private final PostMediaRepository postMediaRepository;
    private final SocialPermissionService socialPermissionService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PetAlbumMediaResponse> getPetAlbumImages(Long currentUserId, Long petId, int page, int size) {
        socialPermissionService.checkUserActive(currentUserId);
        validatePetId(petId);
        if (!petRepository.existsById(petId)) {
            throw new ResourceNotFoundException("Pet not found");
        }

        Pageable pageable = buildPageable(page, size);
        Page<PetAlbumMediaResponse> album = postMediaRepository.findVisiblePetAlbumImages(
                        petId,
                        currentUserId,
                        PostStatus.PUBLISHED,
                        MediaType.IMAGE,
                        PostPrivacy.PUBLIC,
                        PostPrivacy.FRIENDS,
                        pageable
                )
                .map(this::toResponse);
        return PageResponse.from(album);
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

    private void validatePetId(Long petId) {
        if (petId == null || petId <= 0) {
            throw new BadRequestException("Pet id must be greater than 0");
        }
    }

    private PetAlbumMediaResponse toResponse(PostMedia media) {
        Post post = media.getPost();
        User owner = post.getUser();
        return PetAlbumMediaResponse.builder()
                .mediaId(media.getId())
                .postId(post.getId())
                .petId(post.getPetId())
                .mediaUrl(media.getMediaUrl())
                .thumbnailUrl(media.getThumbnailUrl())
                .mediaType(media.getMediaType() == null ? null : media.getMediaType().getValue())
                .originalFilename(media.getOriginalFilename())
                .mimeType(media.getMimeType())
                .fileSize(media.getFileSize())
                .displayOrder(media.getDisplayOrder())
                .postCaption(post.getCaption())
                .postPrivacy(post.getPrivacy() == null ? null : post.getPrivacy().getValue())
                .postCreatedAt(post.getCreatedAt())
                .postOwnerId(owner == null ? null : owner.getId())
                .postOwnerName(owner == null ? null : owner.getFullName())
                .postOwnerAvatarUrl(owner == null ? null : owner.getAvatarUrl())
                .build();
    }
}

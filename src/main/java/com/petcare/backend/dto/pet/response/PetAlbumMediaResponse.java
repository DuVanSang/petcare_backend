package com.petcare.backend.dto.pet.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PetAlbumMediaResponse {
    private Long mediaId;
    private Long postId;
    private Long petId;

    private String mediaUrl;
    private String thumbnailUrl;
    private String mediaType;
    private String originalFilename;
    private String mimeType;
    private Long fileSize;
    private Integer displayOrder;

    private String postCaption;
    private String postPrivacy;
    private LocalDateTime postCreatedAt;

    private Long postOwnerId;
    private String postOwnerName;
    private String postOwnerAvatarUrl;
}

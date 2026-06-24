package com.petcare.backend.service;

import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.dto.pet.response.PetAlbumMediaResponse;

public interface PetAlbumService {
    PageResponse<PetAlbumMediaResponse> getPetAlbumImages(Long currentUserId, Long petId, int page, int size);
}

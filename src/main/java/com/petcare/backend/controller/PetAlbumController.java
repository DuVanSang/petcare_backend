package com.petcare.backend.controller;

import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.dto.pet.response.PetAlbumMediaResponse;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.PetAlbumService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PetAlbumController {
    private final PetAlbumService petAlbumService;

    @GetMapping("/pets/{petId}/album")
    public ResponseEntity<ApiResponse<PageResponse<PetAlbumMediaResponse>>> getPetAlbum(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long petId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Pet album fetched successfully",
                petAlbumService.getPetAlbumImages(principal.getId(), petId, page, size)
        ));
    }
}

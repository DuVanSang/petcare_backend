package com.petcare.backend.controller;

import com.petcare.backend.dto.admin.category.request.AdminCreateBreedRequest;
import com.petcare.backend.dto.admin.category.request.AdminCreateSpeciesRequest;
import com.petcare.backend.dto.admin.category.request.AdminUpdateBreedRequest;
import com.petcare.backend.dto.admin.category.request.AdminUpdateSpeciesRequest;
import com.petcare.backend.dto.admin.category.response.AdminBreedResponse;
import com.petcare.backend.dto.admin.category.response.AdminSpeciesResponse;
import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.service.AdminCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
@Tag(name = "Admin - Categories", description = "Quản lý loài và giống")
@SecurityRequirement(name = "bearerAuth")
public class AdminCategoryController {
    private final AdminCategoryService adminCategoryService;

    @GetMapping("/species")
    @Operation(summary = "Lấy danh sách loài")
    public ResponseEntity<ApiResponse<PageResponse<AdminSpeciesResponse>>> getSpecies(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách loài thành công",
                adminCategoryService.getSpecies(keyword, active, page, size)
        ));
    }

    @GetMapping("/species/{speciesId}")
    @Operation(summary = "Xem chi tiết loài")
    public ResponseEntity<ApiResponse<AdminSpeciesResponse>> getSpeciesDetail(@PathVariable Long speciesId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy chi tiết loài thành công",
                adminCategoryService.getSpeciesDetail(speciesId)
        ));
    }

    @PostMapping("/species")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo loài")
    public ResponseEntity<ApiResponse<AdminSpeciesResponse>> createSpecies(
            @Valid @RequestBody AdminCreateSpeciesRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Tạo loài thành công",
                adminCategoryService.createSpecies(request)
        ));
    }

    @PatchMapping("/species/{speciesId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật loài")
    public ResponseEntity<ApiResponse<AdminSpeciesResponse>> updateSpecies(
            @PathVariable Long speciesId,
            @Valid @RequestBody AdminUpdateSpeciesRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật loài thành công",
                adminCategoryService.updateSpecies(speciesId, request)
        ));
    }

    @GetMapping("/breeds")
    @Operation(summary = "Lấy danh sách giống")
    public ResponseEntity<ApiResponse<PageResponse<AdminBreedResponse>>> getBreeds(
            @RequestParam(required = false) Long speciesId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách giống thành công",
                adminCategoryService.getBreeds(speciesId, keyword, active, page, size)
        ));
    }

    @GetMapping("/breeds/{breedId}")
    @Operation(summary = "Xem chi tiết giống")
    public ResponseEntity<ApiResponse<AdminBreedResponse>> getBreedDetail(@PathVariable Long breedId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy chi tiết giống thành công",
                adminCategoryService.getBreedDetail(breedId)
        ));
    }

    @PostMapping("/breeds")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo giống")
    public ResponseEntity<ApiResponse<AdminBreedResponse>> createBreed(
            @Valid @RequestBody AdminCreateBreedRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Tạo giống thành công",
                adminCategoryService.createBreed(request)
        ));
    }

    @PatchMapping("/breeds/{breedId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật giống")
    public ResponseEntity<ApiResponse<AdminBreedResponse>> updateBreed(
            @PathVariable Long breedId,
            @Valid @RequestBody AdminUpdateBreedRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật giống thành công",
                adminCategoryService.updateBreed(breedId, request)
        ));
    }
}

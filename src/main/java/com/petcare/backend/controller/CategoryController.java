package com.petcare.backend.controller;

import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.dto.pet.request.CreateBreedRequest;
import com.petcare.backend.dto.pet.request.CreateSpeciesRequest;
import com.petcare.backend.dto.pet.response.BreedResponse;
import com.petcare.backend.dto.pet.response.SpeciesResponse;
import com.petcare.backend.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Danh mục loài và giống thú cưng")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/species")
    @Operation(summary = "Lấy danh sách loài thú cưng (dropdown)")
    public ResponseEntity<ApiResponse<PageResponse<SpeciesResponse>>> getAllSpecies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách loài thành công",
                categoryService.getAllSpecies(page, size)
        ));
    }

    @PostMapping("/species")
    @Operation(summary = "Tạo loài mới (tự thêm mục Khác cho dropdown giống)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<SpeciesResponse>> createSpecies(
            @Valid @RequestBody CreateSpeciesRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo loài thành công", categoryService.createSpecies(request)));
    }

    @GetMapping("/species/{speciesId}/breeds")
    @Operation(summary = "Lấy danh sách giống theo loài (dropdown)")
    public ResponseEntity<ApiResponse<PageResponse<BreedResponse>>> getBreeds(
            @PathVariable Long speciesId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách giống thành công",
                categoryService.getBreedsBySpecies(speciesId, page, size)
        ));
    }

    @PostMapping("/species/{speciesId}/breeds")
    @Operation(summary = "Tạo giống mới cho một loài")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<BreedResponse>> createBreed(
            @PathVariable Long speciesId,
            @Valid @RequestBody CreateBreedRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo giống thành công", categoryService.createBreed(speciesId, request)));
    }
}

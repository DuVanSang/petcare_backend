package com.petcare.backend.controller;

import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.dto.pet.response.BreedResponse;
import com.petcare.backend.dto.pet.response.SpeciesResponse;
import com.petcare.backend.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Danh mục loài và giống thú cưng")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/species")
    @Operation(summary = "Lấy danh sách loài thú cưng")
    public ResponseEntity<ApiResponse<List<SpeciesResponse>>> getAllSpecies() {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách loài thành công",
                categoryService.getAllSpecies()
        ));
    }

    @GetMapping("/species/{speciesId}/breeds")
    @Operation(summary = "Lấy danh sách giống theo loài")
    public ResponseEntity<ApiResponse<List<BreedResponse>>> getBreeds(@PathVariable Long speciesId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách giống thành công",
                categoryService.getBreedsBySpecies(speciesId)
        ));
    }
}

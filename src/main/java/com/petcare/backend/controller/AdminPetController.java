package com.petcare.backend.controller;

import com.petcare.backend.dto.admin.pet.response.AdminPetDetailResponse;
import com.petcare.backend.dto.admin.pet.response.AdminPetResponse;
import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.dto.vaccination.response.VaccinationResponse;
import com.petcare.backend.model.Pet;
import com.petcare.backend.model.PetVaccination;
import com.petcare.backend.service.AdminPetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/pets")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
@Tag(name = "Admin - Pets", description = "Xem dữ liệu thú cưng và hồ sơ tiêm")
@SecurityRequirement(name = "bearerAuth")
public class AdminPetController {
    private final AdminPetService adminPetService;

    @GetMapping
    @Operation(summary = "Lấy danh sách thú cưng")
    public ResponseEntity<ApiResponse<PageResponse<AdminPetResponse>>> getPets(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) Long speciesId,
            @RequestParam(required = false) Pet.PetStatus status,
            @RequestParam(required = false) Pet.VaccinePlanStatus vaccinePlanStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách thú cưng thành công",
                adminPetService.getPets(keyword, ownerId, speciesId, status, vaccinePlanStatus, page, size)
        ));
    }

    @GetMapping("/{petId}")
    @Operation(summary = "Xem chi tiết thú cưng")
    public ResponseEntity<ApiResponse<AdminPetDetailResponse>> getPetDetail(@PathVariable Long petId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy chi tiết thú cưng thành công",
                adminPetService.getPetDetail(petId)
        ));
    }

    @GetMapping("/{petId}/vaccinations")
    @Operation(summary = "Xem hồ sơ tiêm của thú cưng")
    public ResponseEntity<ApiResponse<List<VaccinationResponse>>> getPetVaccinations(
            @PathVariable Long petId,
            @RequestParam(required = false) PetVaccination.VaccinationStatus status
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy hồ sơ tiêm của thú cưng thành công",
                adminPetService.getPetVaccinations(petId, status)
        ));
    }
}

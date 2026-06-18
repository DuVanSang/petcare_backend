package com.petcare.backend.controller;

import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.dto.vaccination.request.CompleteVaccinationRequest;
import com.petcare.backend.dto.vaccination.request.RescheduleVaccinationRequest;
import com.petcare.backend.dto.vaccination.request.SkipVaccinationRequest;
import com.petcare.backend.dto.vaccination.response.VaccinationResponse;
import com.petcare.backend.model.PetVaccination;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.VaccinationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pets/{petId}/vaccinations")
@RequiredArgsConstructor
@Tag(name = "Vaccinations", description = "Quản lý lịch tiêm phòng và hồ sơ tiêm của thú cưng")
@SecurityRequirement(name = "bearerAuth")
public class VaccinationController {
    private final VaccinationService vaccinationService;

    @GetMapping
    @Operation(summary = "Lấy danh sách lịch tiêm của thú cưng")
    public ResponseEntity<ApiResponse<List<VaccinationResponse>>> getVaccinations(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long petId,
            @RequestParam(required = false) PetVaccination.VaccinationStatus status) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách lịch tiêm thành công",
                vaccinationService.getVaccinations(principal, petId, status)
        ));
    }

    @GetMapping("/{vaccinationId}")
    @Operation(summary = "Xem chi tiết một mũi tiêm")
    public ResponseEntity<ApiResponse<VaccinationResponse>> getVaccination(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long petId,
            @PathVariable Long vaccinationId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy thông tin mũi tiêm thành công",
                vaccinationService.getVaccination(principal, petId, vaccinationId)
        ));
    }

    @PatchMapping("/{vaccinationId}/complete")
    @Operation(summary = "Đánh dấu mũi tiêm đã hoàn thành")
    public ResponseEntity<ApiResponse<VaccinationResponse>> completeVaccination(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long petId,
            @PathVariable Long vaccinationId,
            @Valid @RequestBody CompleteVaccinationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật mũi tiêm đã hoàn thành",
                vaccinationService.completeVaccination(principal, petId, vaccinationId, request)
        ));
    }

    @PatchMapping("/{vaccinationId}/skip")
    @Operation(summary = "Bỏ qua một mũi tiêm")
    public ResponseEntity<ApiResponse<VaccinationResponse>> skipVaccination(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long petId,
            @PathVariable Long vaccinationId,
            @Valid @RequestBody SkipVaccinationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã bỏ qua mũi tiêm",
                vaccinationService.skipVaccination(principal, petId, vaccinationId, request)
        ));
    }

    @PatchMapping("/{vaccinationId}/reschedule")
    @Operation(summary = "Dời lịch tiêm")
    public ResponseEntity<ApiResponse<VaccinationResponse>> rescheduleVaccination(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long petId,
            @PathVariable Long vaccinationId,
            @Valid @RequestBody RescheduleVaccinationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Dời lịch tiêm thành công",
                vaccinationService.rescheduleVaccination(principal, petId, vaccinationId, request)
        ));
    }
}

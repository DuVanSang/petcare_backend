package com.petcare.backend.controller;

import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.dto.vaccination.request.CompleteVaccinationRequest;
import com.petcare.backend.dto.vaccination.request.ConfirmVaccinationPlanRequest;
import com.petcare.backend.dto.vaccination.request.CreateManualVaccinationRequest;
import com.petcare.backend.dto.vaccination.request.RescheduleVaccinationRequest;
import com.petcare.backend.dto.vaccination.request.SetupVaccinationPlanRequest;
import com.petcare.backend.dto.vaccination.request.SkipVaccinationRequest;
import com.petcare.backend.dto.vaccination.response.VaccineOptionResponse;
import com.petcare.backend.dto.vaccination.response.VaccinationResponse;
import com.petcare.backend.dto.vaccination.response.VaccinationSafetyWarningResponse;
import com.petcare.backend.model.PetVaccination;
import com.petcare.backend.model.VaccineTemplate;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pets/{petId}/vaccinations")
@RequiredArgsConstructor
@Tag(name = "Vaccinations", description = "Quản lý lịch và hồ sơ tiêm phòng")
@SecurityRequirement(name = "bearerAuth")
public class VaccinationController {
    private final VaccinationService vaccinationService;

    @PostMapping("/setup-plan")
    @Operation(summary = "Thiết lập và sinh kế hoạch tiêm đề xuất")
    public ResponseEntity<ApiResponse<List<VaccinationResponse>>> setupPlan(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long petId,
            @Valid @RequestBody SetupVaccinationPlanRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Thiết lập kế hoạch tiêm thành công",
                vaccinationService.setupPlan(principal, petId, request)
        ));
    }

    @PatchMapping("/confirm-plan")
    @Operation(summary = "Xác nhận và kích hoạt kế hoạch tiêm")
    public ResponseEntity<ApiResponse<List<VaccinationResponse>>> confirmPlan(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long petId,
            @Valid @RequestBody ConfirmVaccinationPlanRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Kích hoạt lịch tiêm thành công",
                vaccinationService.confirmPlan(principal, petId, request)
        ));
    }

    @DeleteMapping("/reset-plan")
    @PostMapping("/reset-plan")
    @Operation(summary = "Xóa và thiết lập lại kế hoạch tiêm từ đầu")
    public ResponseEntity<ApiResponse<Void>> resetPlan(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long petId) {
        vaccinationService.resetPlan(principal, petId);
        return ResponseEntity.ok(ApiResponse.success("Đã đặt lại kế hoạch tiêm thành công", null));
    }

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

    @GetMapping("/options")
    @Operation(summary = "Lấy danh sách vaccine có thể chọn")
    public ResponseEntity<ApiResponse<List<VaccineOptionResponse>>> getVaccineOptions(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long petId,
            @RequestParam(required = false) VaccineTemplate.TargetStage targetStage,
            @RequestParam(required = false) String seriesCode) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách vaccine có thể chọn thành công",
                vaccinationService.getVaccineOptions(principal, petId, targetStage, seriesCode)
        ));
    }

    @PostMapping
    @Operation(summary = "Tạo mũi tiêm thủ công từ vaccine hệ thống")
    public ResponseEntity<ApiResponse<VaccinationResponse>> createManualVaccination(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long petId,
            @Valid @RequestBody CreateManualVaccinationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Tạo mũi tiêm thủ công thành công",
                vaccinationService.createManualVaccination(principal, petId, request)
        ));
    }

    @GetMapping("/{vaccinationId}")
    @Operation(summary = "Lấy chi tiết một mũi tiêm")
    public ResponseEntity<ApiResponse<VaccinationResponse>> getVaccination(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long petId,
            @PathVariable Long vaccinationId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy thông tin mũi tiêm thành công",
                vaccinationService.getVaccination(principal, petId, vaccinationId)
        ));
    }

    @GetMapping("/{vaccinationId}/safety-check")
    @Operation(summary = "Kiểm tra cảnh báo an toàn trước khi tiêm")
    public ResponseEntity<ApiResponse<VaccinationSafetyWarningResponse>> checkSafety(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long petId,
            @PathVariable Long vaccinationId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Kiểm tra cảnh báo an toàn thành công",
                vaccinationService.checkSafety(principal, petId, vaccinationId)
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
    @Operation(summary = "Dời ngày dự kiến của một mũi tiêm")
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

    @PatchMapping("/schedule-mode")
    @Operation(summary = "Chuyển đổi chế độ lịch tiêm (Tự động / Thủ công)")
    public ResponseEntity<ApiResponse<String>> switchScheduleMode(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long petId,
            @RequestParam com.petcare.backend.model.Pet.VaccineScheduleMode mode) {
        return ResponseEntity.ok(ApiResponse.success(
                vaccinationService.switchScheduleMode(principal, petId, mode),
                mode.name()
        ));
    }

    @DeleteMapping("/{vaccinationId}")
    @Operation(summary = "Xóa một mũi tiêm (chỉ cho phép ở chế độ thủ công)")
    public ResponseEntity<ApiResponse<Void>> deleteVaccination(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long petId,
            @PathVariable Long vaccinationId) {
        vaccinationService.deleteVaccination(principal, petId, vaccinationId);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa mũi tiêm thành công", null));
    }
}

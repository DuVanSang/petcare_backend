package com.petcare.backend.controller;

import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.dto.health.request.CreateHealthLogRequest;
import com.petcare.backend.dto.health.request.UpdateHealthLogRequest;
import com.petcare.backend.dto.health.response.HealthLogResponse;
import com.petcare.backend.dto.health.response.TimelineEventResponse;
import com.petcare.backend.dto.health.response.WeightLogResponse;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.HealthLogService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Health Tracking", description = "Theo dõi sức khỏe và cân nặng thú cưng")
@SecurityRequirement(name = "bearerAuth")
public class HealthLogController {

    private final HealthLogService healthLogService;

    @PostMapping("/health-logs")
    @Operation(summary = "Ghi nhật ký sức khỏe và cân nặng")
    public ResponseEntity<ApiResponse<HealthLogResponse>> createHealthLog(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateHealthLogRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Ghi nhật ký sức khỏe thành công",
                healthLogService.createHealthLog(principal, request)
        ));
    }

    @PutMapping("/health-logs/{logId}")
    @Operation(summary = "Cập nhật nhật ký sức khỏe")
    public ResponseEntity<ApiResponse<HealthLogResponse>> updateHealthLog(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long logId,
            @Valid @RequestBody UpdateHealthLogRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật nhật ký sức khỏe thành công",
                healthLogService.updateHealthLog(principal, logId, request)
        ));
    }

    @DeleteMapping("/health-logs/{logId}")
    @Operation(summary = "Xóa nhật ký sức khỏe")
    public ResponseEntity<ApiResponse<Void>> deleteHealthLog(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long logId) {
        healthLogService.deleteHealthLog(principal, logId);
        return ResponseEntity.ok(ApiResponse.success("Xóa nhật ký sức khỏe thành công", null));
    }

    @GetMapping("/pets/{petId}/health-logs")
    @Operation(summary = "Lấy danh sách nhật ký sức khỏe của thú cưng")
    public ResponseEntity<ApiResponse<List<HealthLogResponse>>> getHealthLogs(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long petId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách nhật ký sức khỏe thành công",
                healthLogService.getHealthLogs(principal, petId)
        ));
    }

    @GetMapping("/pets/{petId}/weight-logs")
    @Operation(summary = "Lấy lịch sử cân nặng")
    public ResponseEntity<ApiResponse<List<WeightLogResponse>>> getWeightLogs(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long petId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy lịch sử cân nặng thành công",
                healthLogService.getWeightLogs(principal, petId)
        ));
    }

    @GetMapping("/pets/{petId}/timeline")
    @Operation(summary = "Lấy dòng thời gian sự kiện của thú cưng")
    public ResponseEntity<ApiResponse<List<TimelineEventResponse>>> getTimeline(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long petId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy dòng thời gian thành công",
                healthLogService.getTimeline(principal, petId)
        ));
    }
}

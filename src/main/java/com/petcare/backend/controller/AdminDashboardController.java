package com.petcare.backend.controller;

import com.petcare.backend.dto.admin.dashboard.response.AdminDashboardOverviewResponse;
import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
@Tag(name = "Admin - Dashboard", description = "Tổng quan hệ thống")
@SecurityRequirement(name = "bearerAuth")
public class AdminDashboardController {
    private final AdminDashboardService adminDashboardService;

    @GetMapping("/overview")
    @Operation(summary = "Xem tổng quan hệ thống")
    public ResponseEntity<ApiResponse<AdminDashboardOverviewResponse>> getOverview() {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy tổng quan hệ thống thành công",
                adminDashboardService.getOverview()
        ));
    }
}

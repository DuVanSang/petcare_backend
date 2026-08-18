package com.petcare.backend.controller;

import com.petcare.backend.dto.admin.vaccine.request.AdminCreateVaccineTemplateRequest;
import com.petcare.backend.dto.admin.vaccine.request.AdminUpdateVaccineTemplateRequest;
import com.petcare.backend.dto.admin.vaccine.response.AdminVaccineTemplateResponse;
import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.model.VaccineTemplate;
import com.petcare.backend.service.AdminVaccineTemplateService;
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
@RequestMapping("/api/v1/admin/vaccine-templates")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Vaccine Templates", description = "Quản lý cấu hình phác đồ vaccine")
@SecurityRequirement(name = "bearerAuth")
public class AdminVaccineTemplateController {
    private final AdminVaccineTemplateService adminVaccineTemplateService;

    @GetMapping
    @Operation(summary = "Lấy danh sách template vaccine")
    public ResponseEntity<ApiResponse<PageResponse<AdminVaccineTemplateResponse>>> getTemplates(
            @RequestParam(required = false) Long speciesId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String seriesCode,
            @RequestParam(required = false) VaccineTemplate.TargetStage targetStage,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách template vaccine thành công",
                adminVaccineTemplateService.getTemplates(
                        speciesId,
                        keyword,
                        seriesCode,
                        targetStage,
                        active,
                        page,
                        size
                )
        ));
    }

    @GetMapping("/{templateId}")
    @Operation(summary = "Xem chi tiết template vaccine")
    public ResponseEntity<ApiResponse<AdminVaccineTemplateResponse>> getTemplateDetail(
            @PathVariable Long templateId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy chi tiết template vaccine thành công",
                adminVaccineTemplateService.getTemplateDetail(templateId)
        ));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo template vaccine")
    public ResponseEntity<ApiResponse<AdminVaccineTemplateResponse>> createTemplate(
            @Valid @RequestBody AdminCreateVaccineTemplateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Tạo template vaccine thành công",
                adminVaccineTemplateService.createTemplate(request)
        ));
    }

    @PatchMapping("/{templateId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật template vaccine")
    public ResponseEntity<ApiResponse<AdminVaccineTemplateResponse>> updateTemplate(
            @PathVariable Long templateId,
            @Valid @RequestBody AdminUpdateVaccineTemplateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật template vaccine thành công",
                adminVaccineTemplateService.updateTemplate(templateId, request)
        ));
    }
}

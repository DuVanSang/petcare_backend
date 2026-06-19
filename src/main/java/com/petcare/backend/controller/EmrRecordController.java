package com.petcare.backend.controller;

import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.dto.emr.request.CreateEmrRecordRequest;
import com.petcare.backend.dto.emr.response.EmrRecordResponse;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.EmrRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "EMR", description = "Hồ sơ y tế điện tử thú cưng")
@SecurityRequirement(name = "bearerAuth")
public class EmrRecordController {

    private final EmrRecordService emrRecordService;

    @PostMapping("/emr-records")
    @Operation(summary = "Tạo mới hồ sơ EMR & đính kèm tài liệu")
    public ResponseEntity<ApiResponse<EmrRecordResponse>> createEmrRecord(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateEmrRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Tạo hồ sơ EMR thành công",
                emrRecordService.createEmrRecord(principal, request)
        ));
    }

    @GetMapping("/pets/{petId}/emr-records")
    @Operation(summary = "Lấy danh sách hồ sơ EMR của thú cưng")
    public ResponseEntity<ApiResponse<List<EmrRecordResponse>>> getEmrRecordsByPet(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long petId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách hồ sơ EMR thành công",
                emrRecordService.getEmrRecordsByPet(principal, petId)
        ));
    }

    @GetMapping("/emr-records/{emrRecordId}")
    @Operation(summary = "Xem chi tiết một hồ sơ EMR")
    public ResponseEntity<ApiResponse<EmrRecordResponse>> getEmrRecordById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long emrRecordId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy hồ sơ EMR thành công",
                emrRecordService.getEmrRecordById(principal, emrRecordId)
        ));
    }
}

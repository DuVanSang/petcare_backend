package com.petcare.backend.controller;

import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.dto.social.request.CreateSocialReportRequest;
import com.petcare.backend.dto.social.response.SocialReportResponse;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.SocialReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/social/reports")
@RequiredArgsConstructor
public class SocialReportController {
    private final SocialReportService socialReportService;

    @PostMapping
    public ResponseEntity<ApiResponse<SocialReportResponse>> createReport(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateSocialReportRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Gửi báo cáo thành công",
                socialReportService.createReport(principal.getId(), request)
        ));
    }
}

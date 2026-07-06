package com.petcare.backend.service;

import com.petcare.backend.dto.social.request.CreateSocialReportRequest;
import com.petcare.backend.dto.social.response.SocialReportResponse;

public interface SocialReportService {
    SocialReportResponse createReport(Long currentUserId, CreateSocialReportRequest request);
}

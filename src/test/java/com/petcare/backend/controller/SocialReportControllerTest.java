package com.petcare.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.petcare.backend.dto.social.request.CreateSocialReportRequest;
import com.petcare.backend.dto.social.response.SocialReportResponse;
import com.petcare.backend.model.SocialReport;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.SocialReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class SocialReportControllerTest {
    @Mock SocialReportService service;
    @Mock UserPrincipal principal;
    private SocialReportController controller;
    @BeforeEach void setUp() { controller = new SocialReportController(service); when(principal.getId()).thenReturn(1L); }
    @Test void createReportDelegatesAndReturnsCreated() {
        CreateSocialReportRequest request = new CreateSocialReportRequest(); request.setTargetType(SocialReport.ModerationTargetType.post); request.setTargetId(2L); request.setReason(SocialReport.ReportReason.spam);
        when(service.createReport(1L, request)).thenReturn(SocialReportResponse.builder().id(3L).build());
        assertThat(controller.createReport(principal, request).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(service).createReport(1L, request);
    }
}

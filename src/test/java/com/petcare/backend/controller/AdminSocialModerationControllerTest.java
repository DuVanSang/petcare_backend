package com.petcare.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.petcare.backend.dto.admin.social.request.ModerationActionRequest;
import com.petcare.backend.dto.admin.social.request.ResolveSocialReportRequest;
import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.model.SocialReport;
import com.petcare.backend.model.enums.CommentStatus;
import com.petcare.backend.model.enums.PostPrivacy;
import com.petcare.backend.model.enums.PostStatus;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.AdminSocialModerationService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AdminSocialModerationControllerTest {
    @Mock private AdminSocialModerationService service;
    @Mock private UserPrincipal principal;
    @InjectMocks private AdminSocialModerationController controller;

    @Test
    void postEndpoints_DelegateAllFiltersDetailsAndModerationActions() {
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = from.plusDays(1);
        PostStatus status = PostStatus.values()[0];
        PostPrivacy privacy = PostPrivacy.values()[0];
        ModerationActionRequest action = new ModerationActionRequest();
        assertOk(controller.getPosts("keyword", 2L, 3L, status, privacy, from, to, 0, 1));
        assertOk(controller.getPostDetail(4L));
        assertOk(controller.hidePost(principal, 4L, action));
        assertOk(controller.restorePost(principal, 4L, action));
        verify(service).getPosts("keyword", 2L, 3L, status, privacy, from, to, 0, 1);
        verify(service).getPostDetail(4L);
        verify(service).hidePost(principal, 4L, action);
        verify(service).restorePost(principal, 4L, action);
    }

    @Test
    void commentEndpoints_DelegateAllFiltersDetailsAndModerationActions() {
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = from.plusDays(1);
        CommentStatus status = CommentStatus.values()[0];
        ModerationActionRequest action = new ModerationActionRequest();
        assertOk(controller.getComments("keyword", 2L, 3L, status, from, to, 1, 20));
        assertOk(controller.getCommentDetail(5L));
        assertOk(controller.hideComment(principal, 5L, action));
        assertOk(controller.restoreComment(principal, 5L, action));
        verify(service).getComments("keyword", 2L, 3L, status, from, to, 1, 20);
        verify(service).getCommentDetail(5L);
        verify(service).hideComment(principal, 5L, action);
        verify(service).restoreComment(principal, 5L, action);
    }

    @Test
    void reportEndpoints_DelegateFiltersDetailsAndResolutionActions() {
        LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime to = from.plusDays(1);
        SocialReport.ReportStatus status = SocialReport.ReportStatus.values()[0];
        SocialReport.ModerationTargetType target = SocialReport.ModerationTargetType.values()[0];
        SocialReport.ReportReason reason = SocialReport.ReportReason.values()[0];
        ResolveSocialReportRequest request = new ResolveSocialReportRequest();
        assertOk(controller.getReports(status, target, reason, 1L, 2L, from, to, 0, 1));
        assertOk(controller.getReportDetail(6L));
        assertOk(controller.resolveReport(principal, 6L, request));
        assertOk(controller.rejectReport(principal, 6L, request));
        verify(service).getReports(status, target, reason, 1L, 2L, from, to, 0, 1);
        verify(service).getReportDetail(6L);
        verify(service).resolveReport(principal, 6L, request);
        verify(service).rejectReport(principal, 6L, request);
    }

    private void assertOk(ResponseEntity<? extends ApiResponse<?>> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
    }
}

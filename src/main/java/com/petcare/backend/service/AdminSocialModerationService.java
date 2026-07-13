package com.petcare.backend.service;

import com.petcare.backend.dto.admin.social.request.ModerationActionRequest;
import com.petcare.backend.dto.admin.social.request.ResolveSocialReportRequest;
import com.petcare.backend.dto.admin.social.response.AdminSocialCommentResponse;
import com.petcare.backend.dto.admin.social.response.AdminSocialPostResponse;
import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.dto.social.response.SocialReportResponse;
import com.petcare.backend.model.SocialReport;
import com.petcare.backend.model.enums.CommentStatus;
import com.petcare.backend.model.enums.PostPrivacy;
import com.petcare.backend.model.enums.PostStatus;
import com.petcare.backend.security.UserPrincipal;
import java.time.LocalDateTime;

public interface AdminSocialModerationService {
    PageResponse<AdminSocialPostResponse> getPosts(
            String keyword,
            Long authorId,
            Long petId,
            PostStatus status,
            PostPrivacy privacy,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size
    );

    AdminSocialPostResponse getPostDetail(Long postId);

    AdminSocialPostResponse hidePost(UserPrincipal moderator, Long postId, ModerationActionRequest request);

    AdminSocialPostResponse restorePost(UserPrincipal moderator, Long postId, ModerationActionRequest request);

    PageResponse<AdminSocialCommentResponse> getComments(
            String keyword,
            Long postId,
            Long authorId,
            CommentStatus status,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size
    );

    AdminSocialCommentResponse getCommentDetail(Long commentId);

    AdminSocialCommentResponse hideComment(UserPrincipal moderator, Long commentId, ModerationActionRequest request);

    AdminSocialCommentResponse restoreComment(UserPrincipal moderator, Long commentId, ModerationActionRequest request);

    PageResponse<SocialReportResponse> getReports(
            SocialReport.ReportStatus status,
            SocialReport.ModerationTargetType targetType,
            SocialReport.ReportReason reason,
            Long reporterId,
            Long targetId,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size
    );

    SocialReportResponse getReportDetail(Long reportId);

    SocialReportResponse resolveReport(UserPrincipal moderator, Long reportId, ResolveSocialReportRequest request);

    SocialReportResponse rejectReport(UserPrincipal moderator, Long reportId, ResolveSocialReportRequest request);
}

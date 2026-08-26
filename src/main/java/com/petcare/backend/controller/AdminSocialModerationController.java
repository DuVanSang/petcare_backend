package com.petcare.backend.controller;

import com.petcare.backend.dto.admin.social.request.ModerationActionRequest;
import com.petcare.backend.dto.admin.social.request.ResolveSocialReportRequest;
import com.petcare.backend.dto.admin.social.response.AdminSocialCommentResponse;
import com.petcare.backend.dto.admin.social.response.AdminSocialPostResponse;
import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.dto.social.response.SocialReportResponse;
import com.petcare.backend.model.SocialReport;
import com.petcare.backend.model.enums.CommentStatus;
import com.petcare.backend.model.enums.PostPrivacy;
import com.petcare.backend.model.enums.PostStatus;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.AdminSocialModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/social")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Social Moderation", description = "Kiểm duyệt nội dung cộng đồng")
@SecurityRequirement(name = "bearerAuth")
public class AdminSocialModerationController {
    private final AdminSocialModerationService adminSocialModerationService;

    @GetMapping("/posts")
    @Operation(summary = "Lấy danh sách bài viết cộng đồng")
    public ResponseEntity<ApiResponse<PageResponse<AdminSocialPostResponse>>> getPosts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false) PostStatus status,
            @RequestParam(required = false) PostPrivacy privacy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách bài viết cộng đồng thành công",
                adminSocialModerationService.getPosts(keyword, authorId, petId, status, privacy, from, to, page, size)
        ));
    }

    @GetMapping("/posts/{postId}")
    @Operation(summary = "Xem chi tiết bài viết cộng đồng")
    public ResponseEntity<ApiResponse<AdminSocialPostResponse>> getPostDetail(@PathVariable Long postId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy chi tiết bài viết cộng đồng thành công",
                adminSocialModerationService.getPostDetail(postId)
        ));
    }

    @PatchMapping("/posts/{postId}/hide")
    @Operation(summary = "Ẩn bài viết cộng đồng")
    public ResponseEntity<ApiResponse<AdminSocialPostResponse>> hidePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId,
            @Valid @RequestBody(required = false) ModerationActionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Ẩn bài viết cộng đồng thành công",
                adminSocialModerationService.hidePost(principal, postId, request)
        ));
    }

    @PatchMapping("/posts/{postId}/restore")
    @Operation(summary = "Khôi phục bài viết cộng đồng")
    public ResponseEntity<ApiResponse<AdminSocialPostResponse>> restorePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId,
            @Valid @RequestBody(required = false) ModerationActionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Khôi phục bài viết cộng đồng thành công",
                adminSocialModerationService.restorePost(principal, postId, request)
        ));
    }

    @GetMapping("/comments")
    @Operation(summary = "Lấy danh sách bình luận cộng đồng")
    public ResponseEntity<ApiResponse<PageResponse<AdminSocialCommentResponse>>> getComments(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long postId,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) CommentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách bình luận cộng đồng thành công",
                adminSocialModerationService.getComments(keyword, postId, authorId, status, from, to, page, size)
        ));
    }

    @GetMapping("/comments/{commentId}")
    @Operation(summary = "Xem chi tiết bình luận cộng đồng")
    public ResponseEntity<ApiResponse<AdminSocialCommentResponse>> getCommentDetail(@PathVariable Long commentId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy chi tiết bình luận cộng đồng thành công",
                adminSocialModerationService.getCommentDetail(commentId)
        ));
    }

    @PatchMapping("/comments/{commentId}/hide")
    @Operation(summary = "Ẩn bình luận cộng đồng")
    public ResponseEntity<ApiResponse<AdminSocialCommentResponse>> hideComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long commentId,
            @Valid @RequestBody(required = false) ModerationActionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Ẩn bình luận cộng đồng thành công",
                adminSocialModerationService.hideComment(principal, commentId, request)
        ));
    }

    @PatchMapping("/comments/{commentId}/restore")
    @Operation(summary = "Khôi phục bình luận cộng đồng")
    public ResponseEntity<ApiResponse<AdminSocialCommentResponse>> restoreComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long commentId,
            @Valid @RequestBody(required = false) ModerationActionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Khôi phục bình luận cộng đồng thành công",
                adminSocialModerationService.restoreComment(principal, commentId, request)
        ));
    }

    @GetMapping("/reports")
    @Operation(summary = "Lấy danh sách báo cáo cộng đồng")
    public ResponseEntity<ApiResponse<PageResponse<SocialReportResponse>>> getReports(
            @RequestParam(required = false) SocialReport.ReportStatus status,
            @RequestParam(required = false) SocialReport.ModerationTargetType targetType,
            @RequestParam(required = false) SocialReport.ReportReason reason,
            @RequestParam(required = false) Long reporterId,
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách báo cáo cộng đồng thành công",
                adminSocialModerationService.getReports(
                        status,
                        targetType,
                        reason,
                        reporterId,
                        targetId,
                        from,
                        to,
                        page,
                        size
                )
        ));
    }

    @GetMapping("/reports/{reportId}")
    @Operation(summary = "Xem chi tiết báo cáo cộng đồng")
    public ResponseEntity<ApiResponse<SocialReportResponse>> getReportDetail(@PathVariable Long reportId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy chi tiết báo cáo cộng đồng thành công",
                adminSocialModerationService.getReportDetail(reportId)
        ));
    }

    @PatchMapping("/reports/{reportId}/resolve")
    @Operation(summary = "Xử lý báo cáo cộng đồng")
    public ResponseEntity<ApiResponse<SocialReportResponse>> resolveReport(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long reportId,
            @Valid @RequestBody(required = false) ResolveSocialReportRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Xử lý báo cáo cộng đồng thành công",
                adminSocialModerationService.resolveReport(principal, reportId, request)
        ));
    }

    @PatchMapping("/reports/{reportId}/reject")
    @Operation(summary = "Từ chối báo cáo cộng đồng")
    public ResponseEntity<ApiResponse<SocialReportResponse>> rejectReport(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long reportId,
            @Valid @RequestBody(required = false) ResolveSocialReportRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Từ chối báo cáo cộng đồng thành công",
                adminSocialModerationService.rejectReport(principal, reportId, request)
        ));
    }
}

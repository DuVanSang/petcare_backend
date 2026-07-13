package com.petcare.backend.dto.social.response;

import com.petcare.backend.model.SocialReport;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SocialReportResponse {
    private Long id;
    private String targetType;
    private Long targetId;
    private Long reporterId;
    private String reporterName;
    private String reporterEmail;
    private String reason;
    private String description;
    private String status;
    private Long resolvedById;
    private String resolvedByName;
    private String resolutionNote;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SocialReportResponse from(SocialReport report) {
        return SocialReportResponse.builder()
                .id(report.getId())
                .targetType(report.getTargetType() == null ? null : report.getTargetType().name())
                .targetId(report.getTargetId())
                .reporterId(report.getReporter() == null ? null : report.getReporter().getId())
                .reporterName(report.getReporter() == null ? null : report.getReporter().getFullName())
                .reporterEmail(report.getReporter() == null ? null : report.getReporter().getEmail())
                .reason(report.getReason() == null ? null : report.getReason().name())
                .description(report.getDescription())
                .status(report.getStatus() == null ? null : report.getStatus().name())
                .resolvedById(report.getResolvedBy() == null ? null : report.getResolvedBy().getId())
                .resolvedByName(report.getResolvedBy() == null ? null : report.getResolvedBy().getFullName())
                .resolutionNote(report.getResolutionNote())
                .resolvedAt(report.getResolvedAt())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }
}

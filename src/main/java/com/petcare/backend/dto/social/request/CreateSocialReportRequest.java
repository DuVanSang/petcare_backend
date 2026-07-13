package com.petcare.backend.dto.social.request;

import com.petcare.backend.model.SocialReport;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateSocialReportRequest {
    @NotNull(message = "Loại đối tượng bị báo cáo không được để trống")
    private SocialReport.ModerationTargetType targetType;

    @NotNull(message = "Id đối tượng bị báo cáo không được để trống")
    private Long targetId;

    @NotNull(message = "Lý do báo cáo không được để trống")
    private SocialReport.ReportReason reason;

    @Size(max = 1000, message = "Mô tả báo cáo không được vượt quá 1000 ký tự")
    private String description;
}

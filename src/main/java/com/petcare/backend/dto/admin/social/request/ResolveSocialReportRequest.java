package com.petcare.backend.dto.admin.social.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResolveSocialReportRequest {
    private Boolean hideTarget;

    @Size(max = 1000, message = "Ghi chú xử lý không được vượt quá 1000 ký tự")
    private String resolutionNote;
}

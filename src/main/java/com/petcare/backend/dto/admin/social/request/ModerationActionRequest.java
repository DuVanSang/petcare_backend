package com.petcare.backend.dto.admin.social.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModerationActionRequest {
    @Size(max = 1000, message = "Lý do xử lý không được vượt quá 1000 ký tự")
    private String reason;
}

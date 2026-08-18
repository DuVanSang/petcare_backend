package com.petcare.backend.dto.user.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserPreferencesRequest {
    @Size(max = 10, message = "Mã ngôn ngữ không được vượt quá 10 ký tự")
    private String languageCode;

    @Size(max = 50, message = "Múi giờ không được vượt quá 50 ký tự")
    private String timezone;

    private Boolean pushNotificationEnabled;

    private Boolean emailAlertsEnabled;
    private Boolean reminderAlertsEnabled;
    private Boolean publicProfileEnabled;
    private Boolean shareLocationEnabled;

    @Size(max = 20, message = "Quyền riêng tư mặc định không hợp lệ")
    private String postDefaultPrivacy;
}

package com.petcare.backend.dto.reminder.response;

import com.petcare.backend.model.CareReminder;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReminderCategoryResponse {
    private CareReminder.ReminderCategory value;
    private String label;
    private String description;
    private String icon;
    private int sortOrder;
    private boolean requiresVaccination;
    private boolean customReminderSupported;
}

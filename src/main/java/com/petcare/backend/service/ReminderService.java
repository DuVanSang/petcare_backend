package com.petcare.backend.service;

import com.petcare.backend.dto.reminder.request.CreateReminderRequest;
import com.petcare.backend.dto.reminder.request.SnoozeReminderRequest;
import com.petcare.backend.dto.reminder.request.UpdateReminderRequest;
import com.petcare.backend.dto.reminder.response.ReminderLogResponse;
import com.petcare.backend.dto.reminder.response.ReminderResponse;
import com.petcare.backend.security.UserPrincipal;
import java.util.List;

public interface ReminderService {
    ReminderResponse createReminder(UserPrincipal principal, CreateReminderRequest request);

    List<ReminderResponse> getMyReminders(UserPrincipal principal);

    ReminderResponse getReminder(UserPrincipal principal, Long reminderId);

    ReminderResponse updateReminder(UserPrincipal principal, Long reminderId, UpdateReminderRequest request);

    void deleteReminder(UserPrincipal principal, Long reminderId);

    ReminderLogResponse completeReminder(UserPrincipal principal, Long reminderId);

    ReminderLogResponse snoozeReminder(
            UserPrincipal principal,
            Long reminderId,
            SnoozeReminderRequest request
    );

    List<ReminderLogResponse> getReminderLogs(UserPrincipal principal, Long reminderId);
}

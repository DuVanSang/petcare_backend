package com.petcare.backend.service;

import com.petcare.backend.dto.admin.reminder.response.AdminReminderLogResponse;
import com.petcare.backend.dto.admin.reminder.response.AdminVaccinationReminderLogResponse;
import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.model.CareReminder;
import com.petcare.backend.model.CareReminderLog;
import com.petcare.backend.model.VaccinationReminderLog;
import java.time.Instant;

public interface AdminReminderLogService {
    PageResponse<AdminReminderLogResponse> getCustomReminderLogs(
            CareReminderLog.ReminderLogStatus status,
            CareReminder.ReminderCategory category,
            Long petId,
            Long userId,
            Instant from,
            Instant to,
            int page,
            int size
    );

    AdminReminderLogResponse getCustomReminderLogDetail(Long logId);

    PageResponse<AdminVaccinationReminderLogResponse> getVaccinationReminderLogs(
            VaccinationReminderLog.VaccinationReminderStatus status,
            VaccinationReminderLog.VaccinationReminderStage stage,
            Long vaccinationId,
            Long petId,
            Long userId,
            Instant from,
            Instant to,
            int page,
            int size
    );

    AdminVaccinationReminderLogResponse getVaccinationReminderLogDetail(Long logId);
}

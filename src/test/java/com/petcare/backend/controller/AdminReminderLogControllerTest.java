package com.petcare.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.model.CareReminder;
import com.petcare.backend.model.CareReminderLog;
import com.petcare.backend.model.VaccinationReminderLog;
import com.petcare.backend.service.AdminReminderLogService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AdminReminderLogControllerTest {
    @Mock private AdminReminderLogService service;
    @InjectMocks private AdminReminderLogController controller;

    @Test
    void customLogEndpoints_DelegateFiltersPaginationAndDetailId() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-02T00:00:00Z");
        CareReminderLog.ReminderLogStatus status = CareReminderLog.ReminderLogStatus.values()[0];
        CareReminder.ReminderCategory category = CareReminder.ReminderCategory.values()[0];
        assertOk(controller.getCustomReminderLogs(status, category, 1L, 2L, from, to, 0, 1));
        assertOk(controller.getCustomReminderLogDetail(7L));
        verify(service).getCustomReminderLogs(status, category, 1L, 2L, from, to, 0, 1);
        verify(service).getCustomReminderLogDetail(7L);
    }

    @Test
    void vaccinationLogEndpoints_DelegateFiltersPaginationAndDetailId() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-02T00:00:00Z");
        VaccinationReminderLog.VaccinationReminderStatus status = VaccinationReminderLog.VaccinationReminderStatus.values()[0];
        VaccinationReminderLog.VaccinationReminderStage stage = VaccinationReminderLog.VaccinationReminderStage.values()[0];
        assertOk(controller.getVaccinationReminderLogs(status, stage, 3L, 1L, 2L, from, to, 4, 20));
        assertOk(controller.getVaccinationReminderLogDetail(8L));
        verify(service).getVaccinationReminderLogs(status, stage, 3L, 1L, 2L, from, to, 4, 20);
        verify(service).getVaccinationReminderLogDetail(8L);
    }

    private void assertOk(ResponseEntity<? extends ApiResponse<?>> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
    }
}

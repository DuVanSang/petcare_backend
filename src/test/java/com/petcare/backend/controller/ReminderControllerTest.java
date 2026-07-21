package com.petcare.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.dto.reminder.request.CreateReminderRequest;
import com.petcare.backend.dto.reminder.request.ReminderStatusFilter;
import com.petcare.backend.dto.reminder.request.RescheduleReminderRequest;
import com.petcare.backend.dto.reminder.request.SnoozeReminderRequest;
import com.petcare.backend.dto.reminder.request.UpdateReminderRequest;
import com.petcare.backend.dto.reminder.response.ReminderCategoryResponse;
import com.petcare.backend.dto.reminder.response.ReminderLogResponse;
import com.petcare.backend.dto.reminder.response.ReminderResponse;
import com.petcare.backend.model.User;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.ReminderService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReminderControllerTest {
    @Mock private ReminderService reminderService;
    private ReminderController controller;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        controller = new ReminderController(reminderService);
        User user = new User(); user.setId(1L); user.setEmail("owner@example.test"); user.setPasswordHash("hash");
        principal = UserPrincipal.from(user);
    }

    @Test
    void getReminderCategories_ReturnsCategories() {
        List<ReminderCategoryResponse> result = List.of(ReminderCategoryResponse.builder().label("Tắm").build());
        when(reminderService.getReminderCategories()).thenReturn(result);
        ApiResponse<List<ReminderCategoryResponse>> body = controller.getReminderCategories().getBody();
        assertThat(body.isSuccess()).isTrue(); assertThat(body.getData()).isSameAs(result);
        verify(reminderService).getReminderCategories();
    }

    @Test
    void createReminder_ValidRequest_DelegatesToService() {
        CreateReminderRequest request = new CreateReminderRequest(); ReminderResponse response = mock(ReminderResponse.class);
        when(reminderService.createReminder(principal, request)).thenReturn(response);
        assertThat(controller.createReminder(principal, request).getBody().getData()).isSameAs(response);
        verify(reminderService).createReminder(principal, request);
    }

    @Test
    void getMyReminders_AllStatus_DelegatesDefaultPartition() {
        List<ReminderResponse> response = List.of(mock(ReminderResponse.class));
        when(reminderService.getMyReminders(principal, ReminderStatusFilter.all)).thenReturn(response);
        assertThat(controller.getMyReminders(principal, ReminderStatusFilter.all).getBody().getData()).isSameAs(response);
        verify(reminderService).getMyReminders(principal, ReminderStatusFilter.all);
    }

    @Test
    void getReminder_ValidId_ReturnsDetail() {
        ReminderResponse response = mock(ReminderResponse.class); when(reminderService.getReminder(principal, 10L)).thenReturn(response);
        assertThat(controller.getReminder(principal, 10L).getBody().getData()).isSameAs(response);
        verify(reminderService).getReminder(principal, 10L);
    }

    @Test
    void updateReminder_ValidRequest_ReturnsUpdatedReminder() {
        UpdateReminderRequest request = new UpdateReminderRequest(); ReminderResponse response = mock(ReminderResponse.class);
        when(reminderService.updateReminder(principal, 10L, request)).thenReturn(response);
        assertThat(controller.updateReminder(principal, 10L, request).getBody().getData()).isSameAs(response);
        verify(reminderService).updateReminder(principal, 10L, request);
    }

    @Test
    void rescheduleReminder_ValidRequest_ReturnsRescheduledReminder() {
        RescheduleReminderRequest request = new RescheduleReminderRequest(); ReminderResponse response = mock(ReminderResponse.class);
        when(reminderService.rescheduleReminder(principal, 10L, request)).thenReturn(response);
        assertThat(controller.rescheduleReminder(principal, 10L, request).getBody().getData()).isSameAs(response);
        verify(reminderService).rescheduleReminder(principal, 10L, request);
    }

    @Test
    void deleteReminder_ValidId_ReturnsEmptySuccess() {
        ApiResponse<Void> body = controller.deleteReminder(principal, 10L).getBody();
        assertThat(body.isSuccess()).isTrue(); assertThat(body.getData()).isNull();
        verify(reminderService).deleteReminder(principal, 10L);
    }

    @Test
    void completeReminder_ValidId_ReturnsLog() {
        ReminderLogResponse log = mock(ReminderLogResponse.class); when(reminderService.completeReminder(principal, 10L)).thenReturn(log);
        assertThat(controller.completeReminder(principal, 10L).getBody().getData()).isSameAs(log);
        verify(reminderService).completeReminder(principal, 10L);
    }

    @Test
    void snoozeReminder_ValidRequest_ReturnsLog() {
        SnoozeReminderRequest request = new SnoozeReminderRequest(); ReminderLogResponse log = mock(ReminderLogResponse.class);
        when(reminderService.snoozeReminder(principal, 10L, request)).thenReturn(log);
        assertThat(controller.snoozeReminder(principal, 10L, request).getBody().getData()).isSameAs(log);
        verify(reminderService).snoozeReminder(principal, 10L, request);
    }

    @Test
    void getReminderLogs_ValidId_ReturnsLogs() {
        List<ReminderLogResponse> logs = List.of(mock(ReminderLogResponse.class));
        when(reminderService.getReminderLogs(principal, 10L)).thenReturn(logs);
        assertThat(controller.getReminderLogs(principal, 10L).getBody().getData()).isSameAs(logs);
        verify(reminderService).getReminderLogs(principal, 10L);
    }
}

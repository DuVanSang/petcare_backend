package com.petcare.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.petcare.backend.dto.admin.notification.response.AdminNotificationLogResponse;
import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.service.AdminNotificationLogService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class AdminNotificationLogControllerTest {
    @Mock AdminNotificationLogService service;
    private AdminNotificationLogController controller;
    @BeforeEach void setUp() { controller = new AdminNotificationLogController(service); }
    @Test void listAndDetailDelegateToService() {
        PageResponse<AdminNotificationLogResponse> page = PageResponse.from(new PageImpl<>(List.of()));
        AdminNotificationLogResponse detail = AdminNotificationLogResponse.builder().id(3L).build();
        LocalDateTime from = LocalDateTime.now().minusDays(1), to = LocalDateTime.now();
        when(service.getNotificationLogs(1L, "post", "sent", true, from, to, 0, 20)).thenReturn(page);
        when(service.getNotificationLogDetail(3L)).thenReturn(detail);
        assertThat(controller.getNotificationLogs(1L, "post", "sent", true, from, to, 0, 20).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.getNotificationLogDetail(3L).getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(service).getNotificationLogs(1L, "post", "sent", true, from, to, 0, 20);
        verify(service).getNotificationLogDetail(3L);
    }
}

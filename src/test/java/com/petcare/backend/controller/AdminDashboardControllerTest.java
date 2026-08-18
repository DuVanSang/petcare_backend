package com.petcare.backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petcare.backend.dto.admin.dashboard.response.AdminDashboardOverviewResponse;
import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.service.AdminDashboardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AdminDashboardControllerTest {
    @Mock private AdminDashboardService dashboardService;

    @Test
    void getOverviewDelegatesAndReturnsSuccessfulResponse() {
        AdminDashboardOverviewResponse overview = AdminDashboardOverviewResponse.builder()
                .totalUsers(12).activePets(5).notificationsSentToday(8).build();
        when(dashboardService.getOverview()).thenReturn(overview);
        AdminDashboardController controller = new AdminDashboardController(dashboardService);

        ResponseEntity<ApiResponse<AdminDashboardOverviewResponse>> response = controller.getOverview();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Lấy tổng quan hệ thống thành công", response.getBody().getMessage());
        assertEquals(true, response.getBody().isSuccess());
        assertSame(overview, response.getBody().getData());
        verify(dashboardService).getOverview();
    }
}

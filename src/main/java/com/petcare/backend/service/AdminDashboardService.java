package com.petcare.backend.service;

import com.petcare.backend.dto.admin.dashboard.response.AdminDashboardOverviewResponse;

public interface AdminDashboardService {
    AdminDashboardOverviewResponse getOverview();
}

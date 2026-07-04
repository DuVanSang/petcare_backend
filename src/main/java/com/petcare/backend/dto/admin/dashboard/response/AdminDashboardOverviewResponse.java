package com.petcare.backend.dto.admin.dashboard.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminDashboardOverviewResponse {
    private long totalUsers;
    private long activeUsers;
    private long bannedUsers;
    private long newUsersToday;
    private long totalPets;
    private long activePets;
    private long totalVaccinations;
    private long proposedVaccinations;
    private long scheduledVaccinations;
    private long overdueVaccinations;
    private long completedVaccinations;
    private long activeReminders;
    private long notificationsSentToday;
}

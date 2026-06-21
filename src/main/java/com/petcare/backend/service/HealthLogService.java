package com.petcare.backend.service;

import com.petcare.backend.dto.health.request.CreateHealthLogRequest;
import com.petcare.backend.dto.health.response.HealthLogResponse;
import com.petcare.backend.dto.health.response.TimelineEventResponse;
import com.petcare.backend.dto.health.response.WeightLogResponse;
import com.petcare.backend.security.UserPrincipal;

import java.util.List;

public interface HealthLogService {

    HealthLogResponse createHealthLog(UserPrincipal principal, CreateHealthLogRequest request);

    List<HealthLogResponse> getHealthLogs(UserPrincipal principal, Long petId);

    List<WeightLogResponse> getWeightLogs(UserPrincipal principal, Long petId);

    List<TimelineEventResponse> getTimeline(UserPrincipal principal, Long petId);
}

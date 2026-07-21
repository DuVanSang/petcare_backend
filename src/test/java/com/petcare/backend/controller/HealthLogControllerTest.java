package com.petcare.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petcare.backend.dto.health.request.CreateHealthLogRequest;
import com.petcare.backend.dto.health.response.HealthLogResponse;
import com.petcare.backend.dto.health.response.TimelineEventResponse;
import com.petcare.backend.dto.health.response.WeightLogResponse;
import com.petcare.backend.model.HealthLog;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.HealthLogService;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HealthLogControllerTest {
    @Mock private HealthLogService service;
    @Mock private UserPrincipal principal;
    private HealthLogController controller;
    @BeforeEach void setUp() { controller = new HealthLogController(service); }

    @Test void createHealthLog_ValidRequest_DelegatesToService() {
        CreateHealthLogRequest request = validRequest(); HealthLogResponse response = mock(HealthLogResponse.class);
        when(service.createHealthLog(principal, request)).thenReturn(response);
        assertThat(controller.createHealthLog(principal, request).getBody().getData()).isSameAs(response);
        verify(service).createHealthLog(principal, request);
    }

    @Test void getHealthLogs_ValidPetId_ReturnsServiceResults() {
        List<HealthLogResponse> results = List.of(mock(HealthLogResponse.class)); when(service.getHealthLogs(principal, 1L)).thenReturn(results);
        assertThat(controller.getHealthLogs(principal, 1L).getBody().getData()).isSameAs(results); verify(service).getHealthLogs(principal, 1L);
    }

    @Test void getWeightLogs_ValidPetId_ReturnsServiceResults() {
        List<WeightLogResponse> results = List.of(mock(WeightLogResponse.class)); when(service.getWeightLogs(principal, 1L)).thenReturn(results);
        assertThat(controller.getWeightLogs(principal, 1L).getBody().getData()).isSameAs(results); verify(service).getWeightLogs(principal, 1L);
    }

    @Test void getTimeline_ValidPetId_ReturnsServiceResults() {
        List<TimelineEventResponse> results = List.of(mock(TimelineEventResponse.class)); when(service.getTimeline(principal, 1L)).thenReturn(results);
        assertThat(controller.getTimeline(principal, 1L).getBody().getData()).isSameAs(results); verify(service).getTimeline(principal, 1L);
    }

    // BVA/EP: 0.01 is valid lower weight boundary; zero, null and future date are invalid partitions.
    @Test void createHealthLogRequest_ValidationCoversWeightAndDateBoundaries() {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        assertThat(validator.validate(validRequest())).isEmpty();
        CreateHealthLogRequest zero = validRequest(); zero.setWeight(BigDecimal.ZERO);
        CreateHealthLogRequest future = validRequest(); future.setDate(LocalDate.now().plusDays(1));
        CreateHealthLogRequest missing = validRequest(); missing.setPetId(null);
        assertThat(validator.validate(zero)).isNotEmpty(); assertThat(validator.validate(future)).isNotEmpty(); assertThat(validator.validate(missing)).isNotEmpty();
    }

    private CreateHealthLogRequest validRequest() {
        CreateHealthLogRequest request = new CreateHealthLogRequest(); request.setPetId(1L); request.setDate(LocalDate.now()); request.setWeight(new BigDecimal("0.01"));
        request.setAppetite(HealthLog.Appetite.good); request.setActivityLevel(HealthLog.ActivityLevel.active); return request;
    }
}

package com.petcare.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.petcare.backend.dto.admin.vaccine.request.*;
import com.petcare.backend.dto.common.ApiResponse;
import com.petcare.backend.model.VaccineTemplate;
import com.petcare.backend.service.AdminVaccineTemplateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AdminVaccineTemplateControllerTest {
    @Mock private AdminVaccineTemplateService service;
    @InjectMocks private AdminVaccineTemplateController controller;

    @Test
    void allTemplateEndpoints_ReturnCorrectStatusAndDelegateRequests() {
        VaccineTemplate.TargetStage stage = VaccineTemplate.TargetStage.values()[0];
        assertOk(controller.getTemplates(1L, "rabies", "SERIES", stage, true, 0, 1));
        assertOk(controller.getTemplateDetail(2L));
        assertThat(controller.createTemplate(new AdminCreateVaccineTemplateRequest()).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertOk(controller.updateTemplate(2L, new AdminUpdateVaccineTemplateRequest()));
        verify(service).getTemplates(1L, "rabies", "SERIES", stage, true, 0, 1);
        verify(service).getTemplateDetail(2L);
        verify(service).createTemplate(org.mockito.ArgumentMatchers.any(AdminCreateVaccineTemplateRequest.class));
        verify(service).updateTemplate(org.mockito.ArgumentMatchers.eq(2L), org.mockito.ArgumentMatchers.any(AdminUpdateVaccineTemplateRequest.class));
    }

    private void assertOk(ResponseEntity<? extends ApiResponse<?>> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
    }
}

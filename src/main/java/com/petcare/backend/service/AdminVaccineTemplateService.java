package com.petcare.backend.service;

import com.petcare.backend.dto.admin.vaccine.request.AdminCreateVaccineTemplateRequest;
import com.petcare.backend.dto.admin.vaccine.request.AdminUpdateVaccineTemplateRequest;
import com.petcare.backend.dto.admin.vaccine.response.AdminVaccineTemplateResponse;
import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.model.VaccineTemplate;

public interface AdminVaccineTemplateService {
    PageResponse<AdminVaccineTemplateResponse> getTemplates(
            Long speciesId,
            String keyword,
            String seriesCode,
            VaccineTemplate.TargetStage targetStage,
            Boolean active,
            int page,
            int size
    );

    AdminVaccineTemplateResponse getTemplateDetail(Long templateId);

    AdminVaccineTemplateResponse createTemplate(AdminCreateVaccineTemplateRequest request);

    AdminVaccineTemplateResponse updateTemplate(Long templateId, AdminUpdateVaccineTemplateRequest request);
}

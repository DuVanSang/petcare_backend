package com.petcare.backend.service;

import com.petcare.backend.dto.vaccination.request.CompleteVaccinationRequest;
import com.petcare.backend.dto.vaccination.request.ConfirmVaccinationPlanRequest;
import com.petcare.backend.dto.vaccination.request.CreateManualVaccinationRequest;
import com.petcare.backend.dto.vaccination.request.RescheduleVaccinationRequest;
import com.petcare.backend.dto.vaccination.request.SetupVaccinationPlanRequest;
import com.petcare.backend.dto.vaccination.request.SkipVaccinationRequest;
import com.petcare.backend.dto.vaccination.response.VaccineOptionResponse;
import com.petcare.backend.dto.vaccination.response.VaccinationResponse;
import com.petcare.backend.model.PetVaccination;
import com.petcare.backend.model.VaccineTemplate;
import com.petcare.backend.security.UserPrincipal;
import java.util.List;

public interface VaccinationService {
    List<VaccinationResponse> setupPlan(
            UserPrincipal principal,
            Long petId,
            SetupVaccinationPlanRequest request
    );

    List<VaccinationResponse> confirmPlan(
            UserPrincipal principal,
            Long petId,
            ConfirmVaccinationPlanRequest request
    );

    List<VaccinationResponse> getVaccinations(
            UserPrincipal principal,
            Long petId,
            PetVaccination.VaccinationStatus status
    );

    List<VaccineOptionResponse> getVaccineOptions(
            UserPrincipal principal,
            Long petId,
            VaccineTemplate.TargetStage targetStage,
            String seriesCode
    );

    VaccinationResponse createManualVaccination(
            UserPrincipal principal,
            Long petId,
            CreateManualVaccinationRequest request
    );

    VaccinationResponse getVaccination(UserPrincipal principal, Long petId, Long vaccinationId);

    VaccinationResponse completeVaccination(
            UserPrincipal principal,
            Long petId,
            Long vaccinationId,
            CompleteVaccinationRequest request
    );

    VaccinationResponse skipVaccination(
            UserPrincipal principal,
            Long petId,
            Long vaccinationId,
            SkipVaccinationRequest request
    );

    VaccinationResponse rescheduleVaccination(
            UserPrincipal principal,
            Long petId,
            Long vaccinationId,
            RescheduleVaccinationRequest request
    );
}

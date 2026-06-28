package com.petcare.backend.service;

import com.petcare.backend.dto.vaccination.request.CompleteVaccinationRequest;
import com.petcare.backend.dto.vaccination.request.ConfirmVaccinationPlanRequest;
import com.petcare.backend.dto.vaccination.request.RescheduleVaccinationRequest;
import com.petcare.backend.dto.vaccination.request.SetupVaccinationPlanRequest;
import com.petcare.backend.dto.vaccination.request.SkipVaccinationRequest;
import com.petcare.backend.dto.vaccination.response.VaccinationResponse;
import com.petcare.backend.model.PetVaccination;
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

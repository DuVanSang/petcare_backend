package com.petcare.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petcare.backend.dto.vaccination.request.*;
import com.petcare.backend.dto.vaccination.response.*;
import com.petcare.backend.model.*;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.VaccinationService;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VaccinationControllerTest {
    @Mock private VaccinationService service;
    @Mock private UserPrincipal principal;
    private VaccinationController controller;
    @BeforeEach void setUp() { controller = new VaccinationController(service); }

    @Test void setupPlan_DelegatesRequest() { SetupVaccinationPlanRequest r=new SetupVaccinationPlanRequest(); List<VaccinationResponse> v=List.of(mock(VaccinationResponse.class)); when(service.setupPlan(principal,1L,r)).thenReturn(v); assertThat(controller.setupPlan(principal,1L,r).getBody().getData()).isSameAs(v); verify(service).setupPlan(principal,1L,r); }
    @Test void confirmPlan_DelegatesRequest() { ConfirmVaccinationPlanRequest r=new ConfirmVaccinationPlanRequest(); List<VaccinationResponse> v=List.of(mock(VaccinationResponse.class)); when(service.confirmPlan(principal,1L,r)).thenReturn(v); assertThat(controller.confirmPlan(principal,1L,r).getBody().getData()).isSameAs(v); verify(service).confirmPlan(principal,1L,r); }
    @Test void getVaccinations_WithStatus_DelegatesFilter() { List<VaccinationResponse> v=List.of(mock(VaccinationResponse.class)); when(service.getVaccinations(principal,1L,PetVaccination.VaccinationStatus.scheduled)).thenReturn(v); assertThat(controller.getVaccinations(principal,1L,PetVaccination.VaccinationStatus.scheduled).getBody().getData()).isSameAs(v); verify(service).getVaccinations(principal,1L,PetVaccination.VaccinationStatus.scheduled); }
    @Test void getVaccineOptions_WithFilters_Delegates() { List<VaccineOptionResponse> v=List.of(mock(VaccineOptionResponse.class)); when(service.getVaccineOptions(principal,1L,VaccineTemplate.TargetStage.PUPPY,"DHPP")).thenReturn(v); assertThat(controller.getVaccineOptions(principal,1L,VaccineTemplate.TargetStage.PUPPY,"DHPP").getBody().getData()).isSameAs(v); verify(service).getVaccineOptions(principal,1L,VaccineTemplate.TargetStage.PUPPY,"DHPP"); }
    @Test void createManualVaccination_DelegatesRequest() { CreateManualVaccinationRequest r=new CreateManualVaccinationRequest(); VaccinationResponse v=mock(VaccinationResponse.class); when(service.createManualVaccination(principal,1L,r)).thenReturn(v); assertThat(controller.createManualVaccination(principal,1L,r).getBody().getData()).isSameAs(v); verify(service).createManualVaccination(principal,1L,r); }
    @Test void getVaccination_DelegatesIds() { VaccinationResponse v=mock(VaccinationResponse.class); when(service.getVaccination(principal,1L,2L)).thenReturn(v); assertThat(controller.getVaccination(principal,1L,2L).getBody().getData()).isSameAs(v); verify(service).getVaccination(principal,1L,2L); }
    @Test void completeVaccination_DelegatesRequest() { CompleteVaccinationRequest r=new CompleteVaccinationRequest(); VaccinationResponse v=mock(VaccinationResponse.class); when(service.completeVaccination(principal,1L,2L,r)).thenReturn(v); assertThat(controller.completeVaccination(principal,1L,2L,r).getBody().getData()).isSameAs(v); verify(service).completeVaccination(principal,1L,2L,r); }
    @Test void skipVaccination_DelegatesRequest() { SkipVaccinationRequest r=new SkipVaccinationRequest(); VaccinationResponse v=mock(VaccinationResponse.class); when(service.skipVaccination(principal,1L,2L,r)).thenReturn(v); assertThat(controller.skipVaccination(principal,1L,2L,r).getBody().getData()).isSameAs(v); verify(service).skipVaccination(principal,1L,2L,r); }
    @Test void rescheduleVaccination_DelegatesRequest() { RescheduleVaccinationRequest r=new RescheduleVaccinationRequest(); VaccinationResponse v=mock(VaccinationResponse.class); when(service.rescheduleVaccination(principal,1L,2L,r)).thenReturn(v); assertThat(controller.rescheduleVaccination(principal,1L,2L,r).getBody().getData()).isSameAs(v); verify(service).rescheduleVaccination(principal,1L,2L,r); }
}

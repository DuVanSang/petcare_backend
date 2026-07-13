package com.petcare.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petcare.backend.dto.pet.request.VaccinationHistoryRequest;
import com.petcare.backend.dto.vaccination.request.SetupVaccinationPlanRequest;
import com.petcare.backend.dto.vaccination.request.RescheduleVaccinationRequest;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.model.Pet;
import com.petcare.backend.model.PetVaccination;
import com.petcare.backend.model.User;
import com.petcare.backend.repository.PetCoParentRepository;
import com.petcare.backend.repository.PetRepository;
import com.petcare.backend.repository.PetTimelineEventRepository;
import com.petcare.backend.repository.PetVaccinationRepository;
import com.petcare.backend.repository.UserRepository;
import com.petcare.backend.repository.VaccineTemplateRepository;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.VaccineScheduleService;
import com.petcare.backend.service.ReminderSynchronizationService;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VaccinationServiceImplTest {
    @Mock
    private PetRepository petRepository;
    @Mock
    private PetCoParentRepository coParentRepository;
    @Mock
    private PetVaccinationRepository vaccinationRepository;
    @Mock
    private PetTimelineEventRepository timelineEventRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VaccineTemplateRepository vaccineTemplateRepository;
    @Mock
    private VaccineScheduleService vaccineScheduleService;
    @Mock
    private ReminderSynchronizationService reminderSynchronizationService;
    @Mock
    private UserPrincipal principal;

    private VaccinationServiceImpl service;
    private Pet pet;

    @BeforeEach
    void setUp() {
        service = new VaccinationServiceImpl(
                petRepository,
                coParentRepository,
                vaccinationRepository,
                timelineEventRepository,
                userRepository,
                vaccineTemplateRepository,
                vaccineScheduleService,
                reminderSynchronizationService
        );

        User owner = new User();
        owner.setId(10L);
        pet = new Pet();
        pet.setId(20L);
        pet.setOwner(owner);
        pet.setVaccinePlanStatus(Pet.VaccinePlanStatus.NOT_CONFIGURED);

        when(principal.getId()).thenReturn(10L);
        when(petRepository.findByIdAndAccessibleByUserId(20L, 10L)).thenReturn(Optional.of(pet));
    }

    @Test
    void setupPlanGeneratesScheduleForNotConfiguredPet() {
        SetupVaccinationPlanRequest request = setupRequest();
        when(vaccinationRepository.existsByPetId(20L)).thenReturn(false);
        when(vaccinationRepository.findByPetIdOrderByScheduledDateAsc(20L)).thenReturn(List.of());

        assertThat(service.setupPlan(principal, 20L, request)).isEmpty();

        verify(vaccineScheduleService).generateProposedSchedule(pet, request.getHistories());
    }

    @Test
    void setupPlanRejectsPetThatAlreadyHasVaccinationRows() {
        SetupVaccinationPlanRequest request = setupRequest();
        when(vaccinationRepository.existsByPetId(20L)).thenReturn(true);

        assertThatThrownBy(() -> service.setupPlan(principal, 20L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Kế hoạch tiêm của thú cưng đã được thiết lập");

        verify(vaccineScheduleService, never()).generateProposedSchedule(pet, request.getHistories());
    }

    @Test
    void rescheduleSynchronizesVaccinationReminders() {
        PetVaccination vaccination = new PetVaccination();
        vaccination.setId(30L);
        vaccination.setPet(pet);
        vaccination.setStatus(PetVaccination.VaccinationStatus.scheduled);
        vaccination.setScheduledDate(LocalDate.now().plusDays(5));
        when(vaccinationRepository.findByIdAndPetId(30L, 20L)).thenReturn(Optional.of(vaccination));
        when(vaccinationRepository.save(vaccination)).thenReturn(vaccination);
        RescheduleVaccinationRequest request = new RescheduleVaccinationRequest();
        request.setScheduledDate(LocalDate.now().plusDays(10));

        service.rescheduleVaccination(principal, 20L, 30L, request);

        verify(reminderSynchronizationService).rescheduleVaccinationReminders(
                vaccination,
                LocalDate.now().plusDays(5)
        );
    }

    private SetupVaccinationPlanRequest setupRequest() {
        VaccinationHistoryRequest history = new VaccinationHistoryRequest();
        history.setSeriesCode("CANINE_CORE_DHPP");
        history.setStatus(VaccinationHistoryRequest.HistoryStatus.NONE);

        SetupVaccinationPlanRequest request = new SetupVaccinationPlanRequest();
        request.setHistories(List.of(history));
        return request;
    }
}

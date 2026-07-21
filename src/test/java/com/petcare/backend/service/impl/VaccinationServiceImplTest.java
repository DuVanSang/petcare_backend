package com.petcare.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petcare.backend.dto.pet.request.VaccinationHistoryRequest;
import com.petcare.backend.dto.vaccination.request.SetupVaccinationPlanRequest;
import com.petcare.backend.dto.vaccination.request.RescheduleVaccinationRequest;
import com.petcare.backend.dto.vaccination.request.CompleteVaccinationRequest;
import com.petcare.backend.dto.vaccination.request.CreateManualVaccinationRequest;
import com.petcare.backend.dto.vaccination.request.SkipVaccinationRequest;
import com.petcare.backend.dto.vaccination.request.ConfirmVaccinationPlanRequest;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.model.Pet;
import com.petcare.backend.model.PetVaccination;
import com.petcare.backend.model.User;
import com.petcare.backend.model.Species;
import com.petcare.backend.model.PetCoParent;
import com.petcare.backend.model.VaccineTemplate;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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

    @Test void getVaccinations_NullAndExplicitStatus_UseCorrectRepositoryPartition() {
        when(vaccinationRepository.findByPetIdOrderByScheduledDateAsc(20L)).thenReturn(List.of());
        when(vaccinationRepository.findByPetIdAndStatusOrderByScheduledDateAsc(20L, PetVaccination.VaccinationStatus.scheduled)).thenReturn(List.of());
        assertThat(service.getVaccinations(principal,20L,null)).isEmpty();
        assertThat(service.getVaccinations(principal,20L,PetVaccination.VaccinationStatus.scheduled)).isEmpty();
    }

    @Test void options_MissingSpecies_IsRejectedAndFiltersValidTemplates() {
        pet.setSpecies(null);
        assertThatThrownBy(() -> service.getVaccineOptions(principal,20L,null,null)).isInstanceOf(BadRequestException.class);
        Species species=new Species(); species.setId(1L); pet.setSpecies(species);
        VaccineTemplate puppy=template("DHPP", VaccineTemplate.TargetStage.PUPPY, true);
        VaccineTemplate adult=template("RABIES", VaccineTemplate.TargetStage.ADULT, true);
        when(vaccineTemplateRepository.findBySpeciesIdAndActiveTrueAndSeriesCodeIsNotNullOrderBySeriesCodeAscDoseNumberAsc(1L)).thenReturn(List.of(puppy,adult));
        assertThat(service.getVaccineOptions(principal,20L,VaccineTemplate.TargetStage.PUPPY," dhpp ")).hasSize(1);
    }

    @Test void manualVaccination_ValidTemplate_SavesLockedManualSchedule() {
        Species species=new Species(); species.setId(1L); pet.setSpecies(species);
        VaccineTemplate template=template("DHPP", VaccineTemplate.TargetStage.PUPPY,true); template.setSpecies(species);
        when(vaccineTemplateRepository.findById(5L)).thenReturn(Optional.of(template));
        when(vaccinationRepository.save(org.mockito.ArgumentMatchers.any(PetVaccination.class))).thenAnswer(i -> i.getArgument(0));
        CreateManualVaccinationRequest request=new CreateManualVaccinationRequest(); request.setVaccineTemplateId(5L); request.setScheduledDate(LocalDate.now().plusDays(1)); request.setNotes(" note ");
        service.createManualVaccination(principal,20L,request);
        verify(vaccinationRepository).save(org.mockito.ArgumentMatchers.any(PetVaccination.class));
    }

    @Test void manualVaccination_InvalidTemplatePartitions_AreRejected() {
        pet.setSpecies(null); CreateManualVaccinationRequest request=new CreateManualVaccinationRequest(); request.setVaccineTemplateId(5L); request.setScheduledDate(LocalDate.now().plusDays(1));
        assertThatThrownBy(() -> service.createManualVaccination(principal,20L,request)).isInstanceOf(BadRequestException.class);
        Species species=new Species(); species.setId(1L); pet.setSpecies(species);
        when(vaccineTemplateRepository.findById(5L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createManualVaccination(principal,20L,request)).isInstanceOf(BadRequestException.class);
        VaccineTemplate inactive=template("DHPP",VaccineTemplate.TargetStage.PUPPY,false); inactive.setSpecies(species);
        when(vaccineTemplateRepository.findById(5L)).thenReturn(Optional.of(inactive));
        assertThatThrownBy(() -> service.createManualVaccination(principal,20L,request)).isInstanceOf(BadRequestException.class);
    }

    @Test void getVaccination_ExistingVaccination_ReturnsResponse() {
        PetVaccination vaccination=vaccination(PetVaccination.VaccinationStatus.scheduled);
        when(vaccinationRepository.findByIdAndPetId(30L,20L)).thenReturn(Optional.of(vaccination));
        assertThat(service.getVaccination(principal,20L,30L)).isNotNull();
    }

    @Test void completeVaccination_ValidScheduled_SavesSynchronizesAndCreatesTimeline() {
        PetVaccination vaccination=vaccination(PetVaccination.VaccinationStatus.scheduled);
        when(vaccinationRepository.findByIdAndPetId(30L,20L)).thenReturn(Optional.of(vaccination)); when(vaccinationRepository.save(vaccination)).thenReturn(vaccination);
        CompleteVaccinationRequest request=new CompleteVaccinationRequest(); request.setActualDate(LocalDate.now()); request.setAdministeredBy(" vet ");
        service.completeVaccination(principal,20L,30L,request);
        assertThat(vaccination.getStatus()).isEqualTo(PetVaccination.VaccinationStatus.completed);
        verify(reminderSynchronizationService).cancelVaccinationReminders(vaccination); verify(vaccineScheduleService).recalculateAfterCompletion(vaccination);
    }

    @Test void completeVaccination_InvalidStatusPartitions_AreRejected() {
        for (PetVaccination.VaccinationStatus status : List.of(PetVaccination.VaccinationStatus.proposed,PetVaccination.VaccinationStatus.completed,PetVaccination.VaccinationStatus.skipped,PetVaccination.VaccinationStatus.cancelled)) {
            PetVaccination vaccination=vaccination(status); when(vaccinationRepository.findByIdAndPetId(30L,20L)).thenReturn(Optional.of(vaccination));
            assertThatThrownBy(() -> service.completeVaccination(principal,20L,30L,new CompleteVaccinationRequest())).isInstanceOf(BadRequestException.class);
        }
    }

    @Test void skipVaccination_ValidAndInvalidStatusPartitions() {
        PetVaccination vaccination=vaccination(PetVaccination.VaccinationStatus.scheduled); when(vaccinationRepository.findByIdAndPetId(30L,20L)).thenReturn(Optional.of(vaccination)); when(vaccinationRepository.save(vaccination)).thenReturn(vaccination);
        service.skipVaccination(principal,20L,30L,new SkipVaccinationRequest()); assertThat(vaccination.getStatus()).isEqualTo(PetVaccination.VaccinationStatus.skipped);
        for(PetVaccination.VaccinationStatus status:List.of(PetVaccination.VaccinationStatus.completed,PetVaccination.VaccinationStatus.proposed,PetVaccination.VaccinationStatus.cancelled)) { vaccination=vaccination(status); when(vaccinationRepository.findByIdAndPetId(30L,20L)).thenReturn(Optional.of(vaccination)); assertThatThrownBy(() -> service.skipVaccination(principal,20L,30L,new SkipVaccinationRequest())).isInstanceOf(BadRequestException.class); }
    }

    @Test void rescheduleVaccination_ProposedAndScheduledKeepExpectedStatus() {
        for(PetVaccination.VaccinationStatus status:List.of(PetVaccination.VaccinationStatus.proposed,PetVaccination.VaccinationStatus.scheduled)) {
            PetVaccination vaccination=vaccination(status); when(vaccinationRepository.findByIdAndPetId(30L,20L)).thenReturn(Optional.of(vaccination)); when(vaccinationRepository.save(vaccination)).thenReturn(vaccination);
            RescheduleVaccinationRequest request=new RescheduleVaccinationRequest(); request.setScheduledDate(LocalDate.now().plusDays(5)); service.rescheduleVaccination(principal,20L,30L,request);
            assertThat(vaccination.getStatus()).isEqualTo(status);
        }
    }

    @Test void rescheduleVaccination_CompletedOrCancelled_IsRejected() {
        for(PetVaccination.VaccinationStatus status:List.of(PetVaccination.VaccinationStatus.completed,PetVaccination.VaccinationStatus.cancelled)) { PetVaccination vaccination=vaccination(status); when(vaccinationRepository.findByIdAndPetId(30L,20L)).thenReturn(Optional.of(vaccination)); RescheduleVaccinationRequest request=new RescheduleVaccinationRequest(); request.setScheduledDate(LocalDate.now().plusDays(1)); assertThatThrownBy(() -> service.rescheduleVaccination(principal,20L,30L,request)).isInstanceOf(BadRequestException.class); }
    }

    @Test void editorCanEditButViewerCannot() {
        User editor=new User(); editor.setId(11L); PetCoParent relation=new PetCoParent(); relation.setRole(PetCoParent.CoParentRole.editor);
        when(principal.getId()).thenReturn(11L); when(petRepository.findByIdAndAccessibleByUserId(20L,11L)).thenReturn(Optional.of(pet)); when(coParentRepository.findByPetIdAndUserId(20L,11L)).thenReturn(Optional.of(relation)); when(vaccinationRepository.existsByPetId(20L)).thenReturn(false); when(vaccinationRepository.findByPetIdOrderByScheduledDateAsc(20L)).thenReturn(List.of());
        service.setupPlan(principal,20L,setupRequest());
        relation.setRole(PetCoParent.CoParentRole.viewer);
        assertThatThrownBy(() -> service.setupPlan(principal,20L,setupRequest())).isInstanceOf(BadRequestException.class);
    }

    @Test void setupAndConfirmPlan_StatusAndUserPartitions() {
        pet.setVaccinePlanStatus(Pet.VaccinePlanStatus.PROPOSED);
        assertThatThrownBy(() -> service.setupPlan(principal,20L,setupRequest())).isInstanceOf(BadRequestException.class);
        pet.setVaccinePlanStatus(Pet.VaccinePlanStatus.NOT_CONFIGURED);
        when(userRepository.findById(10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.confirmPlan(principal,20L,new ConfirmVaccinationPlanRequest())).isInstanceOf(BadRequestException.class);
        when(userRepository.findById(10L)).thenReturn(Optional.of(pet.getOwner()));
        when(vaccineScheduleService.confirmPlan(org.mockito.ArgumentMatchers.eq(pet),org.mockito.ArgumentMatchers.eq(pet.getOwner()),org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        assertThat(service.confirmPlan(principal,20L,new ConfirmVaccinationPlanRequest())).isEmpty();
    }

    @Test void manualVaccination_DuplicateSeriesDate_IsRejected() {
        Species species=new Species();species.setId(1L);pet.setSpecies(species); VaccineTemplate template=template("DHPP",VaccineTemplate.TargetStage.PUPPY,true);template.setSpecies(species);
        when(vaccineTemplateRepository.findById(5L)).thenReturn(Optional.of(template)); when(vaccinationRepository.existsByPetIdAndSeriesCodeAndScheduledDateAndStatusNot(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any())).thenReturn(true);
        CreateManualVaccinationRequest request=new CreateManualVaccinationRequest();request.setVaccineTemplateId(5L);request.setScheduledDate(LocalDate.now().plusDays(1));
        assertThatThrownBy(() -> service.createManualVaccination(principal,20L,request)).isInstanceOf(BadRequestException.class);
    }

    @Test void manualVaccination_MismatchedSpeciesAndMissingVaccination_AreRejected() {
        Species petSpecies=new Species();petSpecies.setId(1L);pet.setSpecies(petSpecies); Species other=new Species();other.setId(2L);
        VaccineTemplate template=template("DHPP",VaccineTemplate.TargetStage.PUPPY,true);template.setSpecies(other); when(vaccineTemplateRepository.findById(5L)).thenReturn(Optional.of(template));
        CreateManualVaccinationRequest request=new CreateManualVaccinationRequest();request.setVaccineTemplateId(5L);request.setScheduledDate(LocalDate.now().plusDays(1));
        assertThatThrownBy(() -> service.createManualVaccination(principal,20L,request)).isInstanceOf(BadRequestException.class);
        when(vaccinationRepository.findByIdAndPetId(99L,20L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getVaccination(principal,20L,99L)).isInstanceOf(BadRequestException.class);
    }

    @Test void completeVaccination_OverdueStatus_IsAllowed() {
        PetVaccination vaccination=vaccination(PetVaccination.VaccinationStatus.overdue); when(vaccinationRepository.findByIdAndPetId(30L,20L)).thenReturn(Optional.of(vaccination)); when(vaccinationRepository.save(vaccination)).thenReturn(vaccination);
        CompleteVaccinationRequest request=new CompleteVaccinationRequest();request.setActualDate(LocalDate.now());
        service.completeVaccination(principal,20L,30L,request);
        assertThat(vaccination.getStatus()).isEqualTo(PetVaccination.VaccinationStatus.completed);
    }

    @Test void completeVaccination_WithoutActualDate_UsesTimelineFallbackDate() {
        PetVaccination vaccination=vaccination(PetVaccination.VaccinationStatus.scheduled); when(vaccinationRepository.findByIdAndPetId(30L,20L)).thenReturn(Optional.of(vaccination)); when(vaccinationRepository.save(vaccination)).thenReturn(vaccination);
        service.completeVaccination(principal,20L,30L,new CompleteVaccinationRequest());
        verify(timelineEventRepository).save(org.mockito.ArgumentMatchers.any());
    }

    private VaccineTemplate template(String series, VaccineTemplate.TargetStage stage, boolean active) { VaccineTemplate t=new VaccineTemplate(); t.setVaccineName(series);t.setSeriesCode(series);t.setTargetStage(stage);t.setDoseNumber(1);t.setMinimumAgeWeeks(8);t.setRecommendedAgeWeeks(8);t.setActive(active);return t; }
    private PetVaccination vaccination(PetVaccination.VaccinationStatus status) { PetVaccination v=new PetVaccination();v.setId(30L);v.setPet(pet);v.setVaccineName("DHPP");v.setStatus(status);v.setScheduledDate(LocalDate.now().plusDays(2));return v; }

    private SetupVaccinationPlanRequest setupRequest() {
        VaccinationHistoryRequest history = new VaccinationHistoryRequest();
        history.setSeriesCode("CANINE_CORE_DHPP");
        history.setStatus(VaccinationHistoryRequest.HistoryStatus.NONE);

        SetupVaccinationPlanRequest request = new SetupVaccinationPlanRequest();
        request.setHistories(List.of(history));
        return request;
    }
}

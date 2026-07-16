package com.petcare.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petcare.backend.dto.pet.request.VaccinationHistoryRequest;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.model.Notification;
import com.petcare.backend.model.Pet;
import com.petcare.backend.model.PetVaccination;
import com.petcare.backend.model.Species;
import com.petcare.backend.model.User;
import com.petcare.backend.model.VaccineTemplate;
import com.petcare.backend.repository.NotificationRepository;
import com.petcare.backend.repository.PetRepository;
import com.petcare.backend.repository.PetTimelineEventRepository;
import com.petcare.backend.repository.PetVaccinationRepository;
import com.petcare.backend.repository.VaccineTemplateRepository;
import com.petcare.backend.service.PushNotificationSender;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VaccineScheduleServiceImplTest {
    @Mock
    private VaccineTemplateRepository templateRepository;
    @Mock
    private PetVaccinationRepository vaccinationRepository;
    @Mock
    private PetRepository petRepository;
    @Mock
    private PetTimelineEventRepository timelineEventRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private PushNotificationSender pushNotificationSender;

    private VaccineScheduleServiceImpl service;
    private Pet pet;

    @BeforeEach
    void setUp() {
        service = new VaccineScheduleServiceImpl(
                templateRepository,
                vaccinationRepository,
                petRepository,
                timelineEventRepository,
                notificationRepository,
                pushNotificationSender,
                new ObjectMapper()
        );
        Species species = new Species();
        species.setId(1L);
        User owner = new User();
        owner.setId(10L);
        pet = new Pet();
        pet.setId(20L);
        pet.setName("Nâu");
        pet.setSpecies(species);
        pet.setOwner(owner);
        lenient().when(vaccinationRepository.save(any(PetVaccination.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void puppyAtEightWeeksCreatesRemainingPuppySeriesAsProposed() {
        pet.setDateOfBirth(LocalDate.now().minusWeeks(8));
        List<VaccineTemplate> templates = List.of(
                template("CANINE_CORE_DHPP", VaccineTemplate.TargetStage.PUPPY, 1, 8, 0, null),
                template("CANINE_CORE_DHPP", VaccineTemplate.TargetStage.PUPPY, 2, 12, 28, null),
                template("CANINE_CORE_DHPP", VaccineTemplate.TargetStage.PUPPY, 3, 16, 28, null),
                template("CANINE_CORE_DHPP", VaccineTemplate.TargetStage.PUPPY, 4, 26, 70, null)
        );
        when(templateRepository
                .findBySpeciesIdAndActiveTrueAndSeriesCodeIsNotNullOrderBySeriesCodeAscDoseNumberAsc(1L))
                .thenReturn(templates);

        service.generateProposedSchedule(pet, List.of(history(
                "CANINE_CORE_DHPP", VaccinationHistoryRequest.HistoryStatus.NONE, 0, null)));

        ArgumentCaptor<PetVaccination> captor = ArgumentCaptor.forClass(PetVaccination.class);
        verify(vaccinationRepository, org.mockito.Mockito.times(4)).save(captor.capture());
        assertThat(captor.getAllValues())
                .allMatch(item -> item.getStatus() == PetVaccination.VaccinationStatus.proposed);
        assertThat(captor.getAllValues().get(2).getScheduledDate())
                .isAfterOrEqualTo(pet.getDateOfBirth().plusWeeks(16));
        assertThat(pet.getVaccinePlanStatus()).isEqualTo(Pet.VaccinePlanStatus.PROPOSED);
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void setupFailsClearlyWhenSpeciesHasNoVaccineTemplates() {
        pet.getSpecies().setId(99L);
        when(templateRepository
                .findBySpeciesIdAndActiveTrueAndSeriesCodeIsNotNullOrderBySeriesCodeAscDoseNumberAsc(99L))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.generateProposedSchedule(pet, List.of(
                history("UNKNOWN_SERIES", VaccinationHistoryRequest.HistoryStatus.NONE, 0, null)
        )))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Chưa có phác đồ vaccine được cấu hình cho loài thú cưng này");

        verify(vaccinationRepository, never()).save(any(PetVaccination.class));
    }

    @Test
    void puppyAtTwentyWeeksStillReceivesIndependentRabiesDose() {
        pet.setDateOfBirth(LocalDate.now().minusWeeks(20));
        VaccineTemplate rabies = template(
                "CANINE_RABIES", VaccineTemplate.TargetStage.PUPPY, 1, 12, 0, null);
        when(templateRepository
                .findBySpeciesIdAndActiveTrueAndSeriesCodeIsNotNullOrderBySeriesCodeAscDoseNumberAsc(1L))
                .thenReturn(List.of(rabies));

        service.generateProposedSchedule(pet, List.of(history(
                "CANINE_RABIES", VaccinationHistoryRequest.HistoryStatus.NONE, 0, null)));

        ArgumentCaptor<PetVaccination> captor = ArgumentCaptor.forClass(PetVaccination.class);
        verify(vaccinationRepository).save(captor.capture());
        assertThat(captor.getValue().getSeriesCode()).isEqualTo("CANINE_RABIES");
        assertThat(captor.getValue().getTargetStage()).isEqualTo(VaccineTemplate.TargetStage.PUPPY);
        assertThat(captor.getValue().getScheduledDate()).isEqualTo(LocalDate.now());
        assertThat(captor.getValue().getStatus()).isEqualTo(PetVaccination.VaccinationStatus.proposed);
    }

    @Test
    void unvaccinatedPuppyAtThirteenWeeksStartsFromFirstDoseWithoutScheduleCompression() {
        pet.setDateOfBirth(LocalDate.now().minusWeeks(13));
        List<VaccineTemplate> templates = List.of(
                template("CANINE_CORE_DHPP", VaccineTemplate.TargetStage.PUPPY, 1, 8, 0, null),
                template("CANINE_CORE_DHPP", VaccineTemplate.TargetStage.PUPPY, 2, 12, 28, null),
                template("CANINE_CORE_DHPP", VaccineTemplate.TargetStage.PUPPY, 3, 16, 28, null),
                template("CANINE_CORE_DHPP", VaccineTemplate.TargetStage.PUPPY, 4, 26, 70, null)
        );
        when(templateRepository
                .findBySpeciesIdAndActiveTrueAndSeriesCodeIsNotNullOrderBySeriesCodeAscDoseNumberAsc(1L))
                .thenReturn(templates);

        service.generateProposedSchedule(pet, List.of(history(
                "CANINE_CORE_DHPP", VaccinationHistoryRequest.HistoryStatus.NONE, 0, null)));

        ArgumentCaptor<PetVaccination> captor = ArgumentCaptor.forClass(PetVaccination.class);
        verify(vaccinationRepository, org.mockito.Mockito.times(4)).save(captor.capture());
        List<PetVaccination> generated = captor.getAllValues();

        assertThat(generated).extracting(PetVaccination::getDoseNumber)
                .containsExactly(1, 2, 3, 4);
        assertThat(generated.get(0).getScheduledDate()).isEqualTo(LocalDate.now());
        assertThat(generated.get(1).getScheduledDate())
                .isEqualTo(generated.get(0).getScheduledDate().plusDays(28));
        assertThat(generated.get(2).getScheduledDate())
                .isEqualTo(generated.get(1).getScheduledDate().plusDays(28));
        assertThat(generated.get(3).getScheduledDate())
                .isEqualTo(generated.get(2).getScheduledDate().plusDays(70));
    }

    @Test
    void kittenAtEightWeeksCreatesFvrcpAndIndependentRabiesSchedule() {
        pet.getSpecies().setId(2L);
        pet.setName("Miu");
        pet.setDateOfBirth(LocalDate.now().minusWeeks(8));
        List<VaccineTemplate> templates = List.of(
                template("FELINE_CORE_FVRCP", VaccineTemplate.TargetStage.PUPPY, 1, 8, 0, null),
                template("FELINE_CORE_FVRCP", VaccineTemplate.TargetStage.PUPPY, 2, 12, 28, null),
                template("FELINE_CORE_FVRCP", VaccineTemplate.TargetStage.PUPPY, 3, 16, 28, null),
                template("FELINE_CORE_FVRCP", VaccineTemplate.TargetStage.PUPPY, 4, 26, 70, null),
                template("FELINE_RABIES", VaccineTemplate.TargetStage.PUPPY, 1, 12, 0, null)
        );
        when(templateRepository
                .findBySpeciesIdAndActiveTrueAndSeriesCodeIsNotNullOrderBySeriesCodeAscDoseNumberAsc(2L))
                .thenReturn(templates);

        service.generateProposedSchedule(pet, List.of(
                history("FELINE_CORE_FVRCP", VaccinationHistoryRequest.HistoryStatus.NONE, 0, null),
                history("FELINE_RABIES", VaccinationHistoryRequest.HistoryStatus.NONE, 0, null)
        ));

        ArgumentCaptor<PetVaccination> captor = ArgumentCaptor.forClass(PetVaccination.class);
        verify(vaccinationRepository, org.mockito.Mockito.times(5)).save(captor.capture());
        List<PetVaccination> generated = captor.getAllValues();

        assertThat(generated).filteredOn(item -> "FELINE_CORE_FVRCP".equals(item.getSeriesCode()))
                .extracting(PetVaccination::getDoseNumber)
                .containsExactly(1, 2, 3, 4);
        assertThat(generated).filteredOn(item -> "FELINE_RABIES".equals(item.getSeriesCode()))
                .singleElement()
                .satisfies(item -> assertThat(item.getScheduledDate())
                        .isEqualTo(pet.getDateOfBirth().plusWeeks(12)));
    }

    @Test
    void unvaccinatedAdultCatCreatesTwoDoseFvrcpCatchUpAndRabies() {
        pet.getSpecies().setId(2L);
        pet.setDateOfBirth(LocalDate.now().minusYears(2));
        List<VaccineTemplate> templates = List.of(
                template("FELINE_CORE_FVRCP", VaccineTemplate.TargetStage.CATCH_UP, 1, 26, 0, null),
                template("FELINE_CORE_FVRCP", VaccineTemplate.TargetStage.CATCH_UP, 2, 26, 28, null),
                template("FELINE_RABIES", VaccineTemplate.TargetStage.CATCH_UP, 1, 26, 0, null)
        );
        when(templateRepository
                .findBySpeciesIdAndActiveTrueAndSeriesCodeIsNotNullOrderBySeriesCodeAscDoseNumberAsc(2L))
                .thenReturn(templates);

        service.generateProposedSchedule(pet, List.of(
                history("FELINE_CORE_FVRCP", VaccinationHistoryRequest.HistoryStatus.NONE, 0, null),
                history("FELINE_RABIES", VaccinationHistoryRequest.HistoryStatus.NONE, 0, null)
        ));

        ArgumentCaptor<PetVaccination> captor = ArgumentCaptor.forClass(PetVaccination.class);
        verify(vaccinationRepository, org.mockito.Mockito.times(3)).save(captor.capture());
        List<PetVaccination> generated = captor.getAllValues();
        List<PetVaccination> fvrcp = generated.stream()
                .filter(item -> "FELINE_CORE_FVRCP".equals(item.getSeriesCode()))
                .toList();

        assertThat(fvrcp).hasSize(2);
        assertThat(fvrcp.get(0).getScheduledDate()).isEqualTo(LocalDate.now());
        assertThat(fvrcp.get(1).getScheduledDate()).isEqualTo(LocalDate.now().plusDays(28));
        assertThat(generated).filteredOn(item -> "FELINE_RABIES".equals(item.getSeriesCode()))
                .singleElement()
                .satisfies(item -> assertThat(item.getScheduledDate()).isEqualTo(LocalDate.now()));
    }

    @Test
    void completeAdultCatHistoryCreatesFvrcpAndRabiesBoostersIndependently() {
        pet.getSpecies().setId(2L);
        pet.setDateOfBirth(LocalDate.now().minusYears(3));
        List<VaccineTemplate> templates = List.of(
                template("FELINE_CORE_FVRCP", VaccineTemplate.TargetStage.ADULT, 1, 26, 0, 36),
                template("FELINE_RABIES", VaccineTemplate.TargetStage.ADULT, 1, 26, 0, 12)
        );
        when(templateRepository
                .findBySpeciesIdAndActiveTrueAndSeriesCodeIsNotNullOrderBySeriesCodeAscDoseNumberAsc(2L))
                .thenReturn(templates);

        service.generateProposedSchedule(pet, List.of(
                history("FELINE_CORE_FVRCP", VaccinationHistoryRequest.HistoryStatus.COMPLETE,
                        3, LocalDate.now().minusMonths(12)),
                history("FELINE_RABIES", VaccinationHistoryRequest.HistoryStatus.COMPLETE,
                        1, LocalDate.now().minusMonths(6))
        ));

        ArgumentCaptor<PetVaccination> captor = ArgumentCaptor.forClass(PetVaccination.class);
        verify(vaccinationRepository, org.mockito.Mockito.times(2)).save(captor.capture());

        assertThat(captor.getAllValues())
                .filteredOn(item -> "FELINE_CORE_FVRCP".equals(item.getSeriesCode()))
                .singleElement()
                .satisfies(item -> assertThat(item.getScheduledDate()).isEqualTo(LocalDate.now().plusMonths(24)));
        assertThat(captor.getAllValues())
                .filteredOn(item -> "FELINE_RABIES".equals(item.getSeriesCode()))
                .singleElement()
                .satisfies(item -> assertThat(item.getScheduledDate()).isEqualTo(LocalDate.now().plusMonths(6)));
    }

    @Test
    void completeAdultHistoryUsesAdultBoosterEvenWhenCompletedDosesIsGreaterThanOne() {
        pet.setDateOfBirth(LocalDate.now().minusYears(2));
        VaccineTemplate adult = template(
                "CANINE_CORE_DHPP", VaccineTemplate.TargetStage.ADULT, 1, 26, 0, 36);
        when(templateRepository
                .findBySpeciesIdAndActiveTrueAndSeriesCodeIsNotNullOrderBySeriesCodeAscDoseNumberAsc(1L))
                .thenReturn(List.of(adult));

        service.generateProposedSchedule(pet, List.of(history(
                "CANINE_CORE_DHPP",
                VaccinationHistoryRequest.HistoryStatus.COMPLETE,
                3,
                LocalDate.now().minusMonths(12)
        )));

        ArgumentCaptor<PetVaccination> captor = ArgumentCaptor.forClass(PetVaccination.class);
        verify(vaccinationRepository).save(captor.capture());
        assertThat(captor.getValue().getTargetStage()).isEqualTo(VaccineTemplate.TargetStage.ADULT);
        assertThat(captor.getValue().getScheduledDate()).isEqualTo(LocalDate.now().plusMonths(24));
    }

    @Test
    void unknownHistoryRequiresReviewAndCreatesConsultationNotification() {
        pet.setDateOfBirth(LocalDate.now().minusYears(2));
        when(templateRepository
                .findBySpeciesIdAndActiveTrueAndSeriesCodeIsNotNullOrderBySeriesCodeAscDoseNumberAsc(1L))
                .thenReturn(List.of(template(
                        "CANINE_RABIES", VaccineTemplate.TargetStage.CATCH_UP, 1, 26, 0, null)));

        service.generateProposedSchedule(pet, null);

        assertThat(pet.getVaccinePlanStatus()).isEqualTo(Pet.VaccinePlanStatus.REVIEW_REQUIRED);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void completionRecalculatesAllUnlockedDownstreamDoses() {
        pet.setDateOfBirth(LocalDate.now().minusWeeks(10));
        pet.setVaccinePlanStatus(Pet.VaccinePlanStatus.ACTIVE);
        PetVaccination completed = vaccination(1, 8, 0, LocalDate.now());
        completed.setPet(pet);
        completed.setActualDate(LocalDate.now());
        completed.setStatus(PetVaccination.VaccinationStatus.completed);
        PetVaccination dose2 = vaccination(2, 12, 28, LocalDate.now().plusDays(5));
        PetVaccination dose3 = vaccination(3, 16, 28, LocalDate.now().plusDays(10));
        dose2.setPet(pet);
        dose3.setPet(pet);
        when(vaccinationRepository
                .findByPetIdAndSeriesCodeAndDoseNumberGreaterThanOrderByDoseNumberAsc(
                        20L, "CANINE_CORE_DHPP", 1))
                .thenReturn(List.of(dose2, dose3));
        service.recalculateAfterCompletion(completed);

        assertThat(dose2.getScheduledDate()).isEqualTo(LocalDate.now().plusDays(28));
        assertThat(dose3.getScheduledDate()).isEqualTo(LocalDate.now().plusDays(56));
    }

    @Test
    void confirmationActivatesAllProposedDoses() {
        User confirmer = new User();
        confirmer.setId(99L);
        PetVaccination dose1 = vaccination(1, 8, 0, LocalDate.now());
        PetVaccination dose2 = vaccination(2, 12, 28, LocalDate.now().plusDays(28));
        dose1.setStatus(PetVaccination.VaccinationStatus.proposed);
        dose2.setStatus(PetVaccination.VaccinationStatus.proposed);
        when(vaccinationRepository.findByPetIdAndStatusOrderBySeriesCodeAscDoseNumberAsc(
                20L, PetVaccination.VaccinationStatus.proposed))
                .thenReturn(List.of(dose1, dose2));

        service.confirmPlan(pet, confirmer, "Đã tham khảo bác sĩ");

        assertThat(List.of(dose1, dose2))
                .allMatch(item -> item.getStatus() == PetVaccination.VaccinationStatus.scheduled)
                .allMatch(item -> item.getConfirmedBy() == confirmer)
                .allMatch(item -> item.getConfirmedAt() != null);
        assertThat(pet.getVaccinePlanStatus()).isEqualTo(Pet.VaccinePlanStatus.ACTIVE);
    }

    @Test
    void lockedDownstreamDoseIsNotOverwritten() {
        pet.setDateOfBirth(LocalDate.now().minusWeeks(10));
        PetVaccination completed = vaccination(1, 8, 0, LocalDate.now());
        completed.setPet(pet);
        completed.setActualDate(LocalDate.now());
        completed.setStatus(PetVaccination.VaccinationStatus.completed);
        PetVaccination locked = vaccination(2, 12, 28, LocalDate.now().plusDays(40));
        locked.setPet(pet);
        locked.setScheduleLocked(true);
        when(vaccinationRepository
                .findByPetIdAndSeriesCodeAndDoseNumberGreaterThanOrderByDoseNumberAsc(
                        20L, "CANINE_CORE_DHPP", 1))
                .thenReturn(List.of(locked));

        service.recalculateAfterCompletion(completed);

        assertThat(locked.getScheduledDate()).isEqualTo(LocalDate.now().plusDays(40));
        verify(vaccinationRepository, never()).save(locked);
    }

    private VaccineTemplate template(
            String seriesCode,
            VaccineTemplate.TargetStage stage,
            int dose,
            int minimumAge,
            int interval,
            Integer boosterMonths) {
        VaccineTemplate template = new VaccineTemplate();
        template.setSeriesCode(seriesCode);
        template.setTargetStage(stage);
        template.setDoseNumber(dose);
        template.setVaccineName(seriesCode + " dose " + dose);
        template.setMinimumAgeWeeks(minimumAge);
        template.setRecommendedAgeWeeks(minimumAge);
        template.setIntervalFromPreviousDays(interval);
        template.setBoosterIntervalMonths(boosterMonths);
        template.setOptional(false);
        template.setActive(true);
        return template;
    }

    private VaccinationHistoryRequest history(
            String seriesCode,
            VaccinationHistoryRequest.HistoryStatus status,
            Integer completedDoses,
            LocalDate lastDate) {
        VaccinationHistoryRequest history = new VaccinationHistoryRequest();
        history.setSeriesCode(seriesCode);
        history.setStatus(status);
        history.setCompletedDoses(completedDoses);
        history.setLastVaccinationDate(lastDate);
        return history;
    }

    private PetVaccination vaccination(
            int dose,
            int minimumAge,
            int interval,
            LocalDate scheduledDate) {
        PetVaccination vaccination = new PetVaccination();
        vaccination.setSeriesCode("CANINE_CORE_DHPP");
        vaccination.setDoseNumber(dose);
        vaccination.setMinimumAgeWeeks(minimumAge);
        vaccination.setIntervalFromPreviousDays(interval);
        vaccination.setScheduledDate(scheduledDate);
        vaccination.setStatus(PetVaccination.VaccinationStatus.scheduled);
        vaccination.setScheduleLocked(false);
        return vaccination;
    }
}

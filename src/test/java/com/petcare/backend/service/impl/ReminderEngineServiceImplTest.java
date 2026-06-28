package com.petcare.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petcare.backend.model.CareReminder;
import com.petcare.backend.model.CareReminderLog;
import com.petcare.backend.model.Notification;
import com.petcare.backend.model.Pet;
import com.petcare.backend.model.PetCoParent;
import com.petcare.backend.model.PetVaccination;
import com.petcare.backend.model.User;
import com.petcare.backend.model.VaccinationReminderLog;
import com.petcare.backend.repository.CareReminderLogRepository;
import com.petcare.backend.repository.CareReminderRepository;
import com.petcare.backend.repository.NotificationRepository;
import com.petcare.backend.repository.PetCoParentRepository;
import com.petcare.backend.repository.PetVaccinationRepository;
import com.petcare.backend.repository.VaccinationReminderLogRepository;
import com.petcare.backend.service.PushNotificationSender;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReminderEngineServiceImplTest {
    @Mock
    private CareReminderRepository reminderRepository;
    @Mock
    private CareReminderLogRepository reminderLogRepository;
    @Mock
    private VaccinationReminderLogRepository vaccinationLogRepository;
    @Mock
    private PetVaccinationRepository vaccinationRepository;
    @Mock
    private PetCoParentRepository coParentRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private PushNotificationSender pushNotificationSender;

    private ReminderEngineServiceImpl service;
    private ReminderScheduleCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new ReminderScheduleCalculator();
        service = new ReminderEngineServiceImpl(
                reminderRepository,
                reminderLogRepository,
                vaccinationLogRepository,
                vaccinationRepository,
                coParentRepository,
                notificationRepository,
                calculator,
                pushNotificationSender,
                new ObjectMapper()
        );
        ReflectionTestUtils.setField(service, "batchSize", 100);
        ReflectionTestUtils.setField(service, "vaccineNotificationTime", "00:00");
        lenient().when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void customWorkerMarksNotifiedAndCreatesNextOccurrenceWithoutWaitingForCompletion() {
        User creator = user(1L);
        Pet pet = pet(10L, creator);
        CareReminder reminder = new CareReminder();
        reminder.setId(100L);
        reminder.setCreatedBy(creator);
        reminder.setPet(pet);
        reminder.setTitle("Tắm cho Milo");
        reminder.setCategory(CareReminder.ReminderCategory.bathing);
        reminder.setStartDate(LocalDate.now().minusWeeks(1));
        reminder.setReminderTime(LocalTime.of(9, 0));
        reminder.setTimezone("Asia/Ho_Chi_Minh");
        reminder.setFrequency(CareReminder.ReminderFrequency.weekly);
        reminder.setActive(true);

        CareReminderLog due = new CareReminderLog();
        due.setId(200L);
        due.setReminder(reminder);
        due.setDueAt(Instant.now().minusSeconds(60));
        due.setDueDate(LocalDate.now());
        due.setStatus(CareReminderLog.ReminderLogStatus.pending);
        when(reminderLogRepository.findDueForUpdate(
                eq(CareReminderLog.ReminderLogStatus.pending),
                any(Instant.class),
                any(Pageable.class)
        )).thenReturn(List.of(due));
        when(reminderLogRepository.existsByReminderIdAndDueAt(eq(100L), any(Instant.class)))
                .thenReturn(false);

        service.processDueCustomReminders();

        assertThat(due.getStatus()).isEqualTo(CareReminderLog.ReminderLogStatus.notified);
        assertThat(due.getNotifiedAt()).isNotNull();
        assertThat(reminder.getNextDueAt()).isEqualTo(calculator.nextDue(reminder, due.getDueAt()));
        verify(notificationRepository).save(any(Notification.class));
        verify(reminderLogRepository, times(2)).save(any(CareReminderLog.class));
        verify(pushNotificationSender).send(any(Notification.class));
    }

    @Test
    void systemVaccinationReminderSendsToOwnerEditorAndViewer() {
        User owner = user(1L);
        User editor = user(2L);
        User viewer = user(3L);
        Pet pet = pet(10L, owner);
        PetVaccination vaccination = vaccination(20L, pet, LocalDate.now());

        PetCoParent editorRelation = coParent(pet, editor, PetCoParent.CoParentRole.editor);
        PetCoParent viewerRelation = coParent(pet, viewer, PetCoParent.CoParentRole.viewer);
        when(vaccinationRepository.findByStatusAndScheduledDateBefore(
                PetVaccination.VaccinationStatus.scheduled,
                LocalDate.now()
        )).thenReturn(List.of());
        when(vaccinationRepository.findByStatusInAndScheduledDateBetween(
                anyList(),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(List.of(vaccination));
        when(coParentRepository.findByPetId(10L)).thenReturn(List.of(editorRelation, viewerRelation));
        when(vaccinationLogRepository.findByVaccinationIdAndUserIdAndStage(
                eq(20L), any(Long.class), eq(VaccinationReminderLog.VaccinationReminderStage.DUE_TODAY)
        )).thenReturn(Optional.empty());
        when(vaccinationLogRepository.save(any(VaccinationReminderLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.processSystemVaccinationReminders();

        verify(notificationRepository, times(3)).save(any(Notification.class));
        verify(pushNotificationSender, times(3)).send(any(Notification.class));
    }

    @Test
    void notifiedVaccinationStageIsNotSentAgain() {
        User owner = user(1L);
        Pet pet = pet(10L, owner);
        PetVaccination vaccination = vaccination(20L, pet, LocalDate.now());
        VaccinationReminderLog existing = new VaccinationReminderLog();
        existing.setStatus(VaccinationReminderLog.VaccinationReminderStatus.notified);

        when(vaccinationRepository.findByStatusAndScheduledDateBefore(
                PetVaccination.VaccinationStatus.scheduled,
                LocalDate.now()
        )).thenReturn(List.of());
        when(vaccinationRepository.findByStatusInAndScheduledDateBetween(
                anyList(),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(List.of(vaccination));
        when(coParentRepository.findByPetId(10L)).thenReturn(List.of());
        when(vaccinationLogRepository.findByVaccinationIdAndUserIdAndStage(
                20L, 1L, VaccinationReminderLog.VaccinationReminderStage.DUE_TODAY
        )).thenReturn(Optional.of(existing));

        service.processSystemVaccinationReminders();

        verify(notificationRepository, never()).save(any(Notification.class));
        verify(pushNotificationSender, never()).send(any(Notification.class));
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setTimezone("Asia/Ho_Chi_Minh");
        return user;
    }

    private Pet pet(Long id, User owner) {
        Pet pet = new Pet();
        pet.setId(id);
        pet.setName("Milo");
        pet.setOwner(owner);
        return pet;
    }

    private PetVaccination vaccination(Long id, Pet pet, LocalDate scheduledDate) {
        PetVaccination vaccination = new PetVaccination();
        vaccination.setId(id);
        vaccination.setPet(pet);
        vaccination.setVaccineName("DHPP");
        vaccination.setScheduledDate(scheduledDate);
        vaccination.setStatus(PetVaccination.VaccinationStatus.scheduled);
        return vaccination;
    }

    private PetCoParent coParent(Pet pet, User user, PetCoParent.CoParentRole role) {
        PetCoParent coParent = new PetCoParent();
        coParent.setPet(pet);
        coParent.setUser(user);
        coParent.setRole(role);
        return coParent;
    }
}

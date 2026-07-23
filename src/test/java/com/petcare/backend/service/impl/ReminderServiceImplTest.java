package com.petcare.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.petcare.backend.dto.reminder.request.CreateReminderRequest;
import com.petcare.backend.dto.reminder.request.ReminderStatusFilter;
import com.petcare.backend.dto.reminder.request.RescheduleReminderRequest;
import com.petcare.backend.dto.reminder.request.SnoozeReminderRequest;
import com.petcare.backend.dto.reminder.request.UpdateReminderRequest;
import com.petcare.backend.dto.reminder.response.ReminderResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.model.CareReminder;
import com.petcare.backend.model.CareReminderLog;
import com.petcare.backend.model.Pet;
import com.petcare.backend.model.PetCoParent;
import com.petcare.backend.model.PetVaccination;
import com.petcare.backend.model.User;
import com.petcare.backend.repository.CareReminderLogRepository;
import com.petcare.backend.repository.CareReminderRepository;
import com.petcare.backend.repository.PetCoParentRepository;
import com.petcare.backend.repository.PetRepository;
import com.petcare.backend.repository.PetVaccinationRepository;
import com.petcare.backend.repository.UserRepository;
import com.petcare.backend.security.UserPrincipal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReminderServiceImplTest {
    @Mock
    private CareReminderRepository reminderRepository;
    @Mock
    private CareReminderLogRepository logRepository;
    @Mock
    private PetRepository petRepository;
    @Mock
    private PetCoParentRepository coParentRepository;
    @Mock
    private PetVaccinationRepository vaccinationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserPrincipal principal;

    private ReminderServiceImpl service;
    private Pet pet;

    @BeforeEach
    void setUp() {
        service = new ReminderServiceImpl(
                reminderRepository,
                logRepository,
                petRepository,
                coParentRepository,
                vaccinationRepository,
                userRepository,
                new ReminderScheduleCalculator()
        );

        User owner = new User();
        owner.setId(1L);
        pet = new Pet();
        pet.setId(10L);
        pet.setName("Milo");
        pet.setOwner(owner);
    }

    @Test
    void viewerCannotCreateCustomReminder() {
        User viewer = new User();
        viewer.setId(2L);
        PetCoParent relation = new PetCoParent();
        relation.setPet(pet);
        relation.setUser(viewer);
        relation.setRole(PetCoParent.CoParentRole.viewer);
        when(principal.getId()).thenReturn(2L);
        when(petRepository.findByIdAndAccessibleByUserId(10L, 2L)).thenReturn(Optional.of(pet));
        when(coParentRepository.findByPetIdAndUserId(10L, 2L)).thenReturn(Optional.of(relation));

        assertThatThrownBy(() -> service.createReminder(principal, bathingRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Bạn không có quyền tạo nhắc nhở cho thú cưng này");

        verify(reminderRepository, never()).save(org.mockito.ArgumentMatchers.any(CareReminder.class));
    }

    @Test
    void proposedVaccinationCannotHaveCustomReminder() {
        User owner = pet.getOwner();
        owner.setTimezone("Asia/Ho_Chi_Minh");
        PetVaccination vaccination = new PetVaccination();
        vaccination.setId(20L);
        vaccination.setPet(pet);
        vaccination.setStatus(PetVaccination.VaccinationStatus.proposed);
        when(principal.getId()).thenReturn(1L);
        when(petRepository.findByIdAndAccessibleByUserId(10L, 1L)).thenReturn(Optional.of(pet));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(vaccinationRepository.findByIdAndPetId(20L, 10L)).thenReturn(Optional.of(vaccination));

        CreateReminderRequest request = bathingRequest();
        request.setCategory(CareReminder.ReminderCategory.vaccination);
        request.setVaccinationId(20L);
        request.setRepeat(CareReminder.ReminderFrequency.once);

        assertThatThrownBy(() -> service.createReminder(principal, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Chỉ có thể tạo nhắc nhở cho mũi tiêm đã lên lịch hoặc quá hạn");
    }

    @Test
    void getMyRemindersCanFilterByOverdueViewStatus() {
        CareReminder overdue = reminder(101L);
        CareReminder upcoming = reminder(102L);
        when(principal.getId()).thenReturn(1L);
        when(reminderRepository.findByCreatedByIdAndActiveTrueOrderByNextDueAtAsc(1L))
                .thenReturn(List.of(overdue, upcoming));
        when(logRepository.existsByReminderIdAndStatusInAndDueAtLessThanEqual(
                eq(101L), anyCollection(), any()
        )).thenReturn(true);
        when(logRepository.existsByReminderIdAndStatusInAndDueAtLessThanEqual(
                eq(102L), anyCollection(), any()
        )).thenReturn(false);
        when(logRepository.existsByReminderIdAndStatusInAndDueAtAfter(
                eq(102L), anyCollection(), any()
        )).thenReturn(true);

        List<ReminderResponse> result = service.getMyReminders(principal, ReminderStatusFilter.overdue);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(101L);
        assertThat(result.get(0).getStatus()).isEqualTo("overdue");
    }

    @Test
    void getReminderCategories_ReturnsAllSupportedCategories() {
        assertThat(service.getReminderCategories()).hasSize(7);
        assertThat(service.getReminderCategories().get(0).isRequiresVaccination()).isTrue();
    }

    // EP: owner creates a non-vaccination reminder; blank timezone falls back to the system default.
    @Test
    void ownerCanCreateNonVaccinationReminderAndPendingLog() {
        User owner = pet.getOwner(); owner.setTimezone(" ");
        when(principal.getId()).thenReturn(1L);
        when(petRepository.findByIdAndAccessibleByUserId(10L, 1L)).thenReturn(Optional.of(pet));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(reminderRepository.save(any(CareReminder.class))).thenAnswer(invocation -> {
            CareReminder value = invocation.getArgument(0); value.setId(99L); return value;
        });
        when(logRepository.save(any(CareReminderLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReminderResponse response = service.createReminder(principal, bathingRequest());

        assertThat(response.getId()).isEqualTo(99L);
        verify(logRepository).save(any(CareReminderLog.class));
    }

    // EP: non-vaccination categories must not reference a vaccination.
    @Test
    void nonVaccinationReminder_WithVaccinationId_IsRejected() {
        arrangeOwnerCreate();
        CreateReminderRequest request = bathingRequest(); request.setVaccinationId(20L);
        assertThatThrownBy(() -> service.createReminder(principal, request)).isInstanceOf(BadRequestException.class)
                .hasMessage("Chỉ nhắc vaccine mới được liên kết với mũi tiêm");
    }

    // EP: vaccination requires an ID and a once frequency.
    @Test
    void vaccinationReminder_MissingIdOrRecurringFrequency_IsRejected() {
        arrangeOwnerCreate();
        CreateReminderRequest noId = bathingRequest(); noId.setCategory(CareReminder.ReminderCategory.vaccination);
        assertThatThrownBy(() -> service.createReminder(principal, noId)).isInstanceOf(BadRequestException.class);
        CreateReminderRequest recurring = bathingRequest(); recurring.setCategory(CareReminder.ReminderCategory.vaccination);
        recurring.setVaccinationId(20L); recurring.setRepeat(CareReminder.ReminderFrequency.daily);
        assertThatThrownBy(() -> service.createReminder(principal, recurring)).isInstanceOf(BadRequestException.class);
    }

    // EP: editor co-parent may create reminders.
    @Test
    void editorCanCreateCustomReminder() {
        User editor = new User(); editor.setId(2L); editor.setTimezone("Asia/Ho_Chi_Minh");
        PetCoParent relation = new PetCoParent(); relation.setRole(PetCoParent.CoParentRole.editor);
        when(principal.getId()).thenReturn(2L);
        when(petRepository.findByIdAndAccessibleByUserId(10L, 2L)).thenReturn(Optional.of(pet));
        when(coParentRepository.findByPetIdAndUserId(10L, 2L)).thenReturn(Optional.of(relation));
        when(userRepository.findById(2L)).thenReturn(Optional.of(editor));
        when(reminderRepository.save(any(CareReminder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createReminder(principal, bathingRequest());
        verify(reminderRepository).save(any(CareReminder.class));
    }

    @Test
    void getMyReminders_ReturnsCompletedAndUpcomingPartitions() {
        CareReminder completed = reminder(101L); CareReminder upcoming = reminder(102L);
        when(principal.getId()).thenReturn(1L);
        when(reminderRepository.findByCreatedByIdAndActiveTrueOrderByNextDueAtAsc(1L)).thenReturn(List.of(completed, upcoming));
        when(logRepository.existsByReminderIdAndStatusInAndDueAtLessThanEqual(any(), anyCollection(), any())).thenReturn(false);
        when(logRepository.existsByReminderIdAndStatusInAndDueAtAfter(eq(101L), org.mockito.ArgumentMatchers.anyCollection(), any())).thenReturn(false);
        when(logRepository.existsByReminderIdAndStatus(101L, CareReminderLog.ReminderLogStatus.completed)).thenReturn(true);
        when(logRepository.existsByReminderIdAndStatusInAndDueAtAfter(eq(102L), org.mockito.ArgumentMatchers.anyCollection(), any())).thenReturn(true);
        assertThat(service.getMyReminders(principal, ReminderStatusFilter.completed)).hasSize(1);
        assertThat(service.getMyReminders(principal, ReminderStatusFilter.all)).hasSize(2);
    }

    @Test
    void getReminder_OwnedReminder_ReturnsDetail() {
        CareReminder reminder = reminder(5L); arrangeOwned(reminder);
        assertThat(service.getReminder(principal, 5L).getId()).isEqualTo(5L);
    }

    // EP: inactive update skips future validation and clears next due while cancelling pending logs.
    @Test
    void updateReminder_Deactivate_ClearsScheduleAndCancelsLogs() {
        CareReminder reminder = reminder(5L); arrangeOwned(reminder); arrangeEditableOwner();
        UpdateReminderRequest request = new UpdateReminderRequest(); request.setActive(false); request.setNotes("  note  ");
        CareReminderLog pending = log(7L, CareReminderLog.ReminderLogStatus.pending);
        when(logRepository.findByReminderIdAndStatusIn(eq(5L), anyCollection())).thenReturn(List.of(pending));
        when(reminderRepository.save(reminder)).thenReturn(reminder);

        service.updateReminder(principal, 5L, request);
        assertThat(reminder.getActive()).isFalse(); assertThat(reminder.getNextDueAt()).isNull();
        assertThat(pending.getStatus()).isEqualTo(CareReminderLog.ReminderLogStatus.cancelled);
    }

    // BVA: end date immediately before start date is invalid.
    @Test
    void updateReminder_EndDateBeforeStartDate_IsRejected() {
        CareReminder reminder = reminder(5L); arrangeOwned(reminder); arrangeEditableOwner();
        UpdateReminderRequest request = new UpdateReminderRequest(); request.setDate(LocalDate.now().plusDays(5));
        request.setEndDate(LocalDate.now().plusDays(4));
        assertThatThrownBy(() -> service.updateReminder(principal, 5L, request)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void rescheduleInactiveReminder_IsRejected() {
        CareReminder reminder = reminder(5L); reminder.setActive(false); arrangeOwned(reminder); arrangeEditableOwner();
        assertThatThrownBy(() -> service.rescheduleReminder(principal, 5L, rescheduleRequest())).isInstanceOf(BadRequestException.class);
    }

    @Test
    void rescheduleActiveReminder_CancelsOldLogAndCreatesNewPendingLog() {
        CareReminder reminder = reminder(5L); arrangeOwned(reminder); arrangeEditableOwner();
        when(logRepository.findByReminderIdAndStatusIn(eq(5L), anyCollection())).thenReturn(List.of(log(7L, CareReminderLog.ReminderLogStatus.snoozed)));
        when(reminderRepository.save(reminder)).thenReturn(reminder);
        service.rescheduleReminder(principal, 5L, rescheduleRequest());
        verify(logRepository, org.mockito.Mockito.atLeastOnce()).save(any(CareReminderLog.class));
    }

    @Test
    void deleteReminder_MarksInactiveAndCancelsLogs() {
        CareReminder reminder = reminder(5L); arrangeOwned(reminder);
        when(logRepository.findByReminderIdAndStatusIn(eq(5L), anyCollection())).thenReturn(List.of());
        service.deleteReminder(principal, 5L);
        assertThat(reminder.getActive()).isFalse(); assertThat(reminder.getNextDueAt()).isNull();
    }

    @Test
    void completeReminder_UsesFutureActionableLogFallback() {
        CareReminder reminder = reminder(5L); arrangeOwned(reminder);
        CareReminderLog log = log(7L, CareReminderLog.ReminderLogStatus.pending);
        when(logRepository.findFirstByReminderIdAndStatusInAndDueAtLessThanEqualOrderByDueAtDesc(eq(5L), anyCollection(), any()))
                .thenReturn(Optional.empty());
        when(logRepository.findFirstByReminderIdAndStatusInOrderByDueAtAsc(eq(5L), anyCollection())).thenReturn(Optional.of(log));
        when(userRepository.findById(1L)).thenReturn(Optional.of(pet.getOwner()));
        when(logRepository.save(log)).thenReturn(log);
        service.completeReminder(principal, 5L);
        assertThat(log.getStatus()).isEqualTo(CareReminderLog.ReminderLogStatus.completed);
    }

    @Test
    void snoozeReminder_DuplicateDueAt_IsRejected() {
        CareReminder reminder = reminder(5L); arrangeOwned(reminder);
        SnoozeReminderRequest request = new SnoozeReminderRequest(); request.setSnoozedUntil(Instant.now().plusSeconds(3600));
        CareReminderLog log = log(7L, CareReminderLog.ReminderLogStatus.pending);
        when(logRepository.findFirstByReminderIdAndStatusInAndDueAtLessThanEqualOrderByDueAtDesc(eq(5L), anyCollection(), any()))
                .thenReturn(Optional.of(log));
        when(logRepository.existsByReminderIdAndDueAtAndIdNot(eq(5L), any(), eq(7L))).thenReturn(true);
        assertThatThrownBy(() -> service.snoozeReminder(principal, 5L, request)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void snoozeReminder_FutureUniqueDueAt_UpdatesReminderAndLog() {
        CareReminder reminder = reminder(5L); arrangeOwned(reminder);
        SnoozeReminderRequest request = new SnoozeReminderRequest(); request.setSnoozedUntil(Instant.now().plusSeconds(3600));
        CareReminderLog log = log(7L, CareReminderLog.ReminderLogStatus.notified);
        when(logRepository.findFirstByReminderIdAndStatusInAndDueAtLessThanEqualOrderByDueAtDesc(eq(5L), anyCollection(), any())).thenReturn(Optional.of(log));
        when(logRepository.existsByReminderIdAndDueAtAndIdNot(eq(5L), any(), eq(7L))).thenReturn(false);
        when(logRepository.save(log)).thenReturn(log);
        service.snoozeReminder(principal, 5L, request);
        assertThat(log.getStatus()).isEqualTo(CareReminderLog.ReminderLogStatus.pending);
        assertThat(reminder.getNextDueAt()).isEqualTo(request.getSnoozedUntil());
    }

    @Test
    void getReminderLogs_OwnedReminder_MapsRepositoryLogs() {
        CareReminder reminder = reminder(5L); arrangeOwned(reminder);
        when(logRepository.findByReminderIdOrderByDueAtDesc(5L)).thenReturn(List.of(log(7L, CareReminderLog.ReminderLogStatus.completed)));
        assertThat(service.getReminderLogs(principal, 5L)).hasSize(1);
    }

    // EP: every reminder category has a title; quarterly/yearly use their special interval values.
    @Test
    void titleAndIntervalHelpers_CoverAllCategoryAndFrequencyPartitions() {
        PetVaccination vaccination = new PetVaccination(); vaccination.setVaccineName("DHPP");
        for (CareReminder.ReminderCategory category : CareReminder.ReminderCategory.values()) {
            String title = ReflectionTestUtils.invokeMethod(service, "buildTitle", category, pet, vaccination);
            assertThat(title).contains("Milo");
        }
        assertThat((Integer) ReflectionTestUtils.invokeMethod(service, "intervalValue", CareReminder.ReminderFrequency.quarterly)).isEqualTo(3);
        assertThat((Integer) ReflectionTestUtils.invokeMethod(service, "intervalValue", CareReminder.ReminderFrequency.yearly)).isEqualTo(12);
        assertThat((Integer) ReflectionTestUtils.invokeMethod(service, "intervalValue", CareReminder.ReminderFrequency.daily)).isEqualTo(1);
    }

    @Test
    void createVaccinationReminder_ScheduledAndOverdueVaccinations_AreAllowed() {
        for (PetVaccination.VaccinationStatus status : List.of(
                PetVaccination.VaccinationStatus.scheduled, PetVaccination.VaccinationStatus.overdue)) {
            arrangeOwnerCreate();
            PetVaccination vaccination = new PetVaccination(); vaccination.setId(20L); vaccination.setPet(pet);
            vaccination.setVaccineName("DHPP"); vaccination.setScheduledDate(LocalDate.now().plusDays(2)); vaccination.setStatus(status);
            when(vaccinationRepository.findByIdAndPetId(20L, 10L)).thenReturn(Optional.of(vaccination));
            when(reminderRepository.save(any(CareReminder.class))).thenAnswer(invocation -> invocation.getArgument(0));
            CreateReminderRequest request = bathingRequest(); request.setCategory(CareReminder.ReminderCategory.vaccination);
            request.setVaccinationId(20L); request.setRepeat(CareReminder.ReminderFrequency.once);
            service.createReminder(principal, request);
        }
        verify(reminderRepository, org.mockito.Mockito.times(2)).save(any(CareReminder.class));
    }

    // EP: active update with supplied values schedules a replacement pending log.
    @Test
    void updateReminder_ActiveWithExplicitValues_ReschedulesAndCreatesLog() {
        CareReminder reminder = reminder(5L); arrangeOwned(reminder); arrangeEditableOwner();
        UpdateReminderRequest request = new UpdateReminderRequest(); request.setDate(LocalDate.now().plusDays(3));
        request.setTime(LocalTime.of(10, 0)); request.setRepeat(CareReminder.ReminderFrequency.monthly);
        request.setEndDate(LocalDate.now().plusDays(5)); request.setActive(true);
        when(logRepository.findByReminderIdAndStatusIn(eq(5L), anyCollection())).thenReturn(List.of());
        when(reminderRepository.save(reminder)).thenReturn(reminder);
        service.updateReminder(principal, 5L, request);
        assertThat(reminder.getNextDueAt()).isNotNull(); assertThat(reminder.getFrequency()).isEqualTo(CareReminder.ReminderFrequency.monthly);
        verify(logRepository).save(any(CareReminderLog.class));
    }

    // EP: vaccination reminders reject recurring update/reschedule, and reschedule rejects an end date before date.
    @Test
    void vaccinationOrInvalidRescheduleConfigurations_AreRejected() {
        CareReminder vaccinationReminder = reminder(5L); vaccinationReminder.setCategory(CareReminder.ReminderCategory.vaccination);
        arrangeOwned(vaccinationReminder); arrangeEditableOwner();
        UpdateReminderRequest update = new UpdateReminderRequest(); update.setRepeat(CareReminder.ReminderFrequency.weekly);
        assertThatThrownBy(() -> service.updateReminder(principal, 5L, update)).isInstanceOf(BadRequestException.class);
        RescheduleReminderRequest recurring = rescheduleRequest(); recurring.setRepeat(CareReminder.ReminderFrequency.daily);
        assertThatThrownBy(() -> service.rescheduleReminder(principal, 5L, recurring)).isInstanceOf(BadRequestException.class);

        CareReminder regular = reminder(5L); arrangeOwned(regular); arrangeEditableOwner();
        RescheduleReminderRequest invalidEnd = rescheduleRequest(); invalidEnd.setEndDate(LocalDate.now().plusDays(1));
        assertThatThrownBy(() -> service.rescheduleReminder(principal, 5L, invalidEnd)).isInstanceOf(BadRequestException.class);
    }

    // BVA: past time is rejected for snooze, and an invalid user timezone is rejected at creation.
    @Test
    void pastDueAndInvalidTimezone_AreRejected() {
        CareReminder reminder = reminder(5L); arrangeOwned(reminder);
        SnoozeReminderRequest snooze = new SnoozeReminderRequest(); snooze.setSnoozedUntil(Instant.now().minusSeconds(1));
        assertThatThrownBy(() -> service.snoozeReminder(principal, 5L, snooze)).isInstanceOf(BadRequestException.class);

        arrangeOwnerCreate(); pet.getOwner().setTimezone("bad-zone");
        assertThatThrownBy(() -> service.createReminder(principal, bathingRequest())).isInstanceOf(BadRequestException.class)
                .hasMessage("Múi giờ người dùng không hợp lệ");
    }

    private void arrangeOwnerCreate() {
        pet.getOwner().setTimezone("Asia/Ho_Chi_Minh"); when(principal.getId()).thenReturn(1L);
        when(petRepository.findByIdAndAccessibleByUserId(10L, 1L)).thenReturn(Optional.of(pet));
        when(userRepository.findById(1L)).thenReturn(Optional.of(pet.getOwner()));
    }

    private void arrangeOwned(CareReminder reminder) {
        when(principal.getId()).thenReturn(1L); when(reminderRepository.findByIdAndCreatedById(5L, 1L)).thenReturn(Optional.of(reminder));
    }

    private void arrangeEditableOwner() {
        when(petRepository.findByIdAndAccessibleByUserId(10L, 1L)).thenReturn(Optional.of(pet));
    }

    private RescheduleReminderRequest rescheduleRequest() {
        RescheduleReminderRequest request = new RescheduleReminderRequest(); request.setDate(LocalDate.now().plusDays(2));
        request.setTime(LocalTime.of(9, 0)); request.setRepeat(CareReminder.ReminderFrequency.weekly); return request;
    }

    private CareReminderLog log(Long id, CareReminderLog.ReminderLogStatus status) {
        CareReminderLog log = new CareReminderLog(); log.setId(id); log.setStatus(status); log.setDueAt(Instant.now().plusSeconds(60)); return log;
    }

    private CreateReminderRequest bathingRequest() {
        CreateReminderRequest request = new CreateReminderRequest();
        request.setPetId(10L);
        request.setCategory(CareReminder.ReminderCategory.bathing);
        request.setDate(LocalDate.now().plusDays(1));
        request.setTime(LocalTime.of(9, 0));
        request.setRepeat(CareReminder.ReminderFrequency.weekly);
        return request;
    }

    private CareReminder reminder(Long id) {
        CareReminder reminder = new CareReminder();
        reminder.setId(id);
        reminder.setPet(pet);
        reminder.setCategory(CareReminder.ReminderCategory.bathing);
        reminder.setTitle("Tắm cho Milo");
        reminder.setStartDate(LocalDate.now().plusDays(1));
        reminder.setReminderTime(LocalTime.of(9, 0));
        reminder.setTimezone("Asia/Ho_Chi_Minh");
        reminder.setFrequency(CareReminder.ReminderFrequency.weekly);
        reminder.setActive(true);
        return reminder;
    }
}

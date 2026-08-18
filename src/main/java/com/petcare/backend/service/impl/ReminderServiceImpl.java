package com.petcare.backend.service.impl;

import com.petcare.backend.dto.reminder.request.CreateReminderRequest;
import com.petcare.backend.dto.reminder.request.RescheduleReminderRequest;
import com.petcare.backend.dto.reminder.request.ReminderStatusFilter;
import com.petcare.backend.dto.reminder.request.SnoozeReminderRequest;
import com.petcare.backend.dto.reminder.request.UpdateReminderRequest;
import com.petcare.backend.dto.reminder.response.ReminderCategoryResponse;
import com.petcare.backend.dto.reminder.response.ReminderLogResponse;
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
import com.petcare.backend.service.ReminderService;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ReminderServiceImpl implements ReminderService {
    private static final Set<CareReminderLog.ReminderLogStatus> ACTIONABLE_STATUSES = Set.of(
            CareReminderLog.ReminderLogStatus.pending,
            CareReminderLog.ReminderLogStatus.notified,
            CareReminderLog.ReminderLogStatus.snoozed
    );
    private static final Set<CareReminderLog.ReminderLogStatus> CANCELLABLE_STATUSES = Set.of(
            CareReminderLog.ReminderLogStatus.pending,
            CareReminderLog.ReminderLogStatus.notified,
            CareReminderLog.ReminderLogStatus.snoozed
    );

    private final CareReminderRepository reminderRepository;
    private final CareReminderLogRepository logRepository;
    private final PetRepository petRepository;
    private final PetCoParentRepository coParentRepository;
    private final PetVaccinationRepository vaccinationRepository;
    private final UserRepository userRepository;
    private final ReminderScheduleCalculator scheduleCalculator;

    @Override
    public List<ReminderCategoryResponse> getReminderCategories() {
        return List.of(
                category(CareReminder.ReminderCategory.vaccination, "Tiêm phòng",
                        "Nhắc lịch tiêm vaccine đã có trong hồ sơ tiêm", "syringe", 1, true),
                category(CareReminder.ReminderCategory.bathing, "Tắm",
                        "Nhắc lịch tắm cho thú cưng", "bath", 2, false),
                category(CareReminder.ReminderCategory.nail_clipping, "Cắt móng",
                        "Nhắc lịch cắt móng cho thú cưng", "scissors", 3, false),
                category(CareReminder.ReminderCategory.deworming, "Tẩy giun",
                        "Nhắc lịch tẩy giun định kỳ", "pill", 4, false),
                category(CareReminder.ReminderCategory.medication, "Uống thuốc",
                        "Nhắc lịch dùng thuốc theo chỉ định", "capsule", 5, false),
                category(CareReminder.ReminderCategory.medical_checkup, "Khám định kỳ",
                        "Nhắc lịch khám sức khỏe định kỳ", "stethoscope", 6, false),
                category(CareReminder.ReminderCategory.other, "Khác",
                        "Nhắc một hoạt động chăm sóc khác", "bell", 7, false)
        );
    }

    @Override
    @Transactional
    public ReminderResponse createReminder(UserPrincipal principal, CreateReminderRequest request) {
        Pet pet = ensureCanEditPet(principal, request.getPetId());
        User creator = getUser(principal.getId());
        String timezone = validTimezone(creator.getTimezone());
        Instant dueAt = scheduleCalculator.toInstant(request.getDate(), request.getTime(), timezone);
        requireFuture(dueAt);

        PetVaccination vaccination = validateVaccinationConfiguration(
                pet, request.getCategory(), request.getVaccinationId(), request.getRepeat()
        );

        CareReminder reminder = new CareReminder();
        reminder.setPet(pet);
        reminder.setCreatedBy(creator);
        reminder.setVaccination(vaccination);
        reminder.setCategory(request.getCategory());
        reminder.setTitle(buildTitle(request.getCategory(), pet, vaccination));
        reminder.setNotes(trimToNull(request.getNotes()));
        reminder.setStartDate(request.getDate());
        reminder.setReminderTime(request.getTime());
        reminder.setTimezone(timezone);
        reminder.setFrequency(request.getRepeat());
        reminder.setNextDueAt(dueAt);
        reminder.setNextDueDate(request.getDate());
        reminder.setEndDate(request.getEndDate());
        reminder.setIntervalValue(intervalValue(request.getRepeat()));
        reminder.setBeforeDurationMinutes(0);
        reminder.setActive(true);
        if (vaccination != null) {
            reminder.setVaccinationOffsetMinutes(calculateVaccinationOffset(vaccination, dueAt, timezone));
        }

        CareReminder saved = reminderRepository.save(reminder);
        createPendingLog(saved, dueAt);
        return ReminderResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReminderResponse> getMyReminders(UserPrincipal principal, ReminderStatusFilter status) {
        return reminderRepository.findByCreatedByIdAndActiveTrueOrderByNextDueAtAsc(principal.getId()).stream()
                .map(reminder -> new ReminderListItem(reminder, resolveViewStatus(reminder)))
                .filter(item -> status == ReminderStatusFilter.all || item.status() == status)
                .map(item -> ReminderResponse.from(item.reminder(), item.status().name()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReminderResponse getReminder(UserPrincipal principal, Long reminderId) {
        CareReminder reminder = getOwnedReminder(principal, reminderId);
        return ReminderResponse.from(reminder, resolveViewStatus(reminder).name());
    }

    @Override
    @Transactional
    public ReminderResponse updateReminder(
            UserPrincipal principal,
            Long reminderId,
            UpdateReminderRequest request) {
        CareReminder reminder = getOwnedReminder(principal, reminderId);
        ensureCanEditPet(principal, reminder.getPet().getId());

        LocalDate date = request.getDate() != null ? request.getDate() : reminder.getStartDate();
        var time = request.getTime() != null ? request.getTime() : reminder.getReminderTime();
        CareReminder.ReminderFrequency frequency = request.getRepeat() != null
                ? request.getRepeat()
                : reminder.getFrequency();
        LocalDate endDate = request.getEndDate() != null ? request.getEndDate() : reminder.getEndDate();
        boolean active = request.getActive() != null ? request.getActive() : Boolean.TRUE.equals(reminder.getActive());

        if (reminder.getCategory() == CareReminder.ReminderCategory.vaccination
                && frequency != CareReminder.ReminderFrequency.once) {
            throw new BadRequestException("Nhắc vaccine tùy chỉnh chỉ được đặt một lần");
        }
        if (endDate != null && endDate.isBefore(date)) {
            throw new BadRequestException("Ngày kết thúc phải từ ngày bắt đầu trở đi");
        }

        Instant dueAt = scheduleCalculator.toInstant(date, time, reminder.getTimezone());
        if (active) {
            requireFuture(dueAt);
        }

        reminder.setStartDate(date);
        reminder.setReminderTime(time);
        reminder.setFrequency(frequency);
        reminder.setEndDate(endDate);
        reminder.setIntervalValue(intervalValue(frequency));
        reminder.setActive(active);
        if (request.getNotes() != null) {
            reminder.setNotes(trimToNull(request.getNotes()));
        }
        cancelOutstandingLogs(reminder.getId());

        if (active) {
            reminder.setNextDueAt(dueAt);
            reminder.setNextDueDate(date);
            if (reminder.getVaccination() != null) {
                reminder.setVaccinationOffsetMinutes(calculateVaccinationOffset(
                        reminder.getVaccination(), dueAt, reminder.getTimezone()
                ));
            }
            createPendingLog(reminder, dueAt);
        } else {
            reminder.setNextDueAt(null);
        }

        return ReminderResponse.from(reminderRepository.save(reminder));
    }

    @Override
    @Transactional
    public ReminderResponse rescheduleReminder(
            UserPrincipal principal,
            Long reminderId,
            RescheduleReminderRequest request) {
        CareReminder reminder = getOwnedReminder(principal, reminderId);
        ensureCanEditPet(principal, reminder.getPet().getId());

        if (!Boolean.TRUE.equals(reminder.getActive())) {
            throw new BadRequestException("Không thể dời lịch nhắc đã bị xóa");
        }
        if (reminder.getCategory() == CareReminder.ReminderCategory.vaccination
                && request.getRepeat() != CareReminder.ReminderFrequency.once) {
            throw new BadRequestException("Nhắc vaccine tùy chỉnh chỉ được đặt một lần");
        }
        if (request.getEndDate() != null && request.getEndDate().isBefore(request.getDate())) {
            throw new BadRequestException("Ngày kết thúc phải từ ngày bắt đầu trở đi");
        }

        Instant dueAt = scheduleCalculator.toInstant(request.getDate(), request.getTime(), reminder.getTimezone());
        requireFuture(dueAt);

        reminder.setStartDate(request.getDate());
        reminder.setReminderTime(request.getTime());
        reminder.setFrequency(request.getRepeat());
        reminder.setEndDate(request.getEndDate());
        reminder.setIntervalValue(intervalValue(request.getRepeat()));
        reminder.setNextDueAt(dueAt);
        reminder.setNextDueDate(request.getDate());
        if (reminder.getVaccination() != null) {
            reminder.setVaccinationOffsetMinutes(calculateVaccinationOffset(
                    reminder.getVaccination(), dueAt, reminder.getTimezone()
            ));
        }

        cancelOutstandingLogs(reminderId);
        createPendingLog(reminder, dueAt);

        return ReminderResponse.from(reminderRepository.save(reminder));
    }

    @Override
    @Transactional
    public void deleteReminder(UserPrincipal principal, Long reminderId) {
        CareReminder reminder = getOwnedReminder(principal, reminderId);
        reminder.setActive(false);
        reminder.setNextDueAt(null);
        reminderRepository.save(reminder);
        cancelOutstandingLogs(reminderId);
    }

    @Override
    @Transactional
    public ReminderLogResponse completeReminder(UserPrincipal principal, Long reminderId) {
        CareReminder reminder = getOwnedReminder(principal, reminderId);
        CareReminderLog log = findCurrentActionableLog(reminderId);
        log.setStatus(CareReminderLog.ReminderLogStatus.completed);
        log.setCompletedAt(Instant.now());
        log.setCompletedBy(getUser(principal.getId()));
        log.setSnoozedUntil(null);
        return ReminderLogResponse.from(logRepository.save(log));
    }

    @Override
    @Transactional
    public ReminderLogResponse snoozeReminder(
            UserPrincipal principal,
            Long reminderId,
            SnoozeReminderRequest request) {
        CareReminder reminder = getOwnedReminder(principal, reminderId);
        Instant snoozedUntil = request.getSnoozedUntil();
        requireFuture(snoozedUntil);

        CareReminderLog log = findCurrentActionableLog(reminderId);
        if (logRepository.existsByReminderIdAndDueAtAndIdNot(
                reminderId, snoozedUntil, log.getId()
        )) {
            throw new BadRequestException("Thời gian báo lại trùng với một lần nhắc đã tồn tại");
        }
        log.setDueAt(snoozedUntil);
        log.setDueDate(scheduleCalculator.toLocalDate(snoozedUntil, reminder.getTimezone()));
        log.setSnoozedUntil(snoozedUntil);
        log.setStatus(CareReminderLog.ReminderLogStatus.pending);
        reminder.setNextDueAt(snoozedUntil);
        reminder.setNextDueDate(log.getDueDate());
        reminderRepository.save(reminder);
        return ReminderLogResponse.from(logRepository.save(log));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReminderLogResponse> getReminderLogs(UserPrincipal principal, Long reminderId) {
        getOwnedReminder(principal, reminderId);
        return logRepository.findByReminderIdOrderByDueAtDesc(reminderId).stream()
                .map(ReminderLogResponse::from)
                .toList();
    }

    private ReminderStatusFilter resolveViewStatus(CareReminder reminder) {
        Instant now = Instant.now();
        Long reminderId = reminder.getId();
        if (logRepository.existsByReminderIdAndStatusInAndDueAtLessThanEqual(
                reminderId, ACTIONABLE_STATUSES, now
        )) {
            return ReminderStatusFilter.overdue;
        }
        if (logRepository.existsByReminderIdAndStatusInAndDueAtAfter(
                reminderId, ACTIONABLE_STATUSES, now
        )) {
            return ReminderStatusFilter.upcoming;
        }
        if (logRepository.existsByReminderIdAndStatus(
                reminderId, CareReminderLog.ReminderLogStatus.completed
        )) {
            return ReminderStatusFilter.completed;
        }
        return ReminderStatusFilter.upcoming;
    }

    private Pet ensureCanEditPet(UserPrincipal principal, Long petId) {
        Pet pet = petRepository.findByIdAndAccessibleByUserId(petId, principal.getId())
                .orElseThrow(() -> new BadRequestException(
                        "Thú cưng không tồn tại hoặc bạn không có quyền truy cập"
                ));
        if (pet.getOwner().getId().equals(principal.getId())) {
            return pet;
        }
        PetCoParent coParent = coParentRepository.findByPetIdAndUserId(petId, principal.getId())
                .orElseThrow(() -> new BadRequestException("Bạn không có quyền tạo nhắc nhở cho thú cưng này"));
        if (coParent.getRole() != PetCoParent.CoParentRole.editor) {
            throw new BadRequestException("Bạn không có quyền tạo nhắc nhở cho thú cưng này");
        }
        return pet;
    }

    private PetVaccination validateVaccinationConfiguration(
            Pet pet,
            CareReminder.ReminderCategory category,
            Long vaccinationId,
            CareReminder.ReminderFrequency frequency) {
        if (category != CareReminder.ReminderCategory.vaccination) {
            if (vaccinationId != null) {
                throw new BadRequestException("Chỉ nhắc vaccine mới được liên kết với mũi tiêm");
            }
            return null;
        }
        if (vaccinationId == null) {
            throw new BadRequestException("Vui lòng chọn mũi tiêm cần nhắc");
        }
        if (frequency != CareReminder.ReminderFrequency.once) {
            throw new BadRequestException("Nhắc vaccine tùy chỉnh chỉ được đặt một lần");
        }
        PetVaccination vaccination = vaccinationRepository.findByIdAndPetId(vaccinationId, pet.getId())
                .orElseThrow(() -> new BadRequestException("Mũi tiêm không thuộc thú cưng đã chọn"));
        if (vaccination.getStatus() != PetVaccination.VaccinationStatus.scheduled
                && vaccination.getStatus() != PetVaccination.VaccinationStatus.overdue) {
            throw new BadRequestException("Chỉ có thể tạo nhắc nhở cho mũi tiêm đã lên lịch hoặc quá hạn");
        }
        return vaccination;
    }

    private CareReminder getOwnedReminder(UserPrincipal principal, Long reminderId) {
        CareReminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new BadRequestException("Nhắc nhở không tồn tại"));

        if (!reminder.getCreatedBy().getId().equals(principal.getId())) {
            ensureCanEditPet(principal, reminder.getPet().getId());
        }
        return reminder;
    }

    private CareReminderLog findCurrentActionableLog(Long reminderId) {
        return logRepository
                .findFirstByReminderIdAndStatusInAndDueAtLessThanEqualOrderByDueAtDesc(
                        reminderId, ACTIONABLE_STATUSES, Instant.now()
                )
                .or(() -> logRepository.findFirstByReminderIdAndStatusInOrderByDueAtAsc(
                        reminderId, ACTIONABLE_STATUSES
                ))
                .orElseThrow(() -> new BadRequestException("Không có lần nhắc nào có thể thao tác"));
    }

    private void createPendingLog(CareReminder reminder, Instant dueAt) {
        CareReminderLog existingLog = logRepository.findByReminderIdAndDueAt(reminder.getId(), dueAt)
                .orElse(null);
        if (existingLog != null) {
            if (existingLog.getStatus() == CareReminderLog.ReminderLogStatus.completed
                    || existingLog.getStatus() == CareReminderLog.ReminderLogStatus.notified) {
                throw new BadRequestException("Thời gian nhắc này đã tồn tại trong lịch sử nhắc nhở");
            }

            existingLog.setStatus(CareReminderLog.ReminderLogStatus.pending);
            existingLog.setDueDate(scheduleCalculator.toLocalDate(dueAt, reminder.getTimezone()));
            existingLog.setNotifiedAt(null);
            existingLog.setCompletedAt(null);
            existingLog.setCompletedBy(null);
            existingLog.setSnoozedUntil(null);
            logRepository.save(existingLog);
            return;
        }

        CareReminderLog log = new CareReminderLog();
        log.setReminder(reminder);
        log.setDueAt(dueAt);
        log.setDueDate(scheduleCalculator.toLocalDate(dueAt, reminder.getTimezone()));
        log.setStatus(CareReminderLog.ReminderLogStatus.pending);
        logRepository.save(log);
    }

    private void cancelOutstandingLogs(Long reminderId) {
        logRepository.findByReminderIdAndStatusIn(reminderId, CANCELLABLE_STATUSES).forEach(log -> {
            log.setStatus(CareReminderLog.ReminderLogStatus.cancelled);
            logRepository.save(log);
        });
    }

    private long calculateVaccinationOffset(PetVaccination vaccination, Instant dueAt, String timezone) {
        Instant vaccinationAnchor = vaccination.getScheduledDate()
                .atStartOfDay(ZoneId.of(timezone))
                .toInstant();
        return Duration.between(vaccinationAnchor, dueAt).toMinutes();
    }

    private String buildTitle(
            CareReminder.ReminderCategory category,
            Pet pet,
            PetVaccination vaccination) {
        return switch (category) {
            case vaccination -> "Nhắc tiêm " + vaccination.getVaccineName() + " cho " + pet.getName();
            case bathing -> "Tắm cho " + pet.getName();
            case nail_clipping -> "Cắt móng cho " + pet.getName();
            case deworming -> "Tẩy giun cho " + pet.getName();
            case medication -> "Cho " + pet.getName() + " uống thuốc";
            case medical_checkup -> "Khám định kỳ cho " + pet.getName();
            case other -> "Chăm sóc " + pet.getName();
        };
    }

    private ReminderCategoryResponse category(
            CareReminder.ReminderCategory value,
            String label,
            String description,
            String icon,
            int sortOrder,
            boolean requiresVaccination) {
        return ReminderCategoryResponse.builder()
                .value(value)
                .label(label)
                .description(description)
                .icon(icon)
                .sortOrder(sortOrder)
                .requiresVaccination(requiresVaccination)
                .customReminderSupported(true)
                .build();
    }

    private int intervalValue(CareReminder.ReminderFrequency frequency) {
        if (frequency == CareReminder.ReminderFrequency.quarterly) {
            return 3;
        }
        if (frequency == CareReminder.ReminderFrequency.yearly) {
            return 12;
        }
        return 1;
    }

    private String validTimezone(String timezone) {
        String value = StringUtils.hasText(timezone) ? timezone : "Asia/Ho_Chi_Minh";
        try {
            ZoneId.of(value);
            return value;
        } catch (Exception ex) {
            throw new BadRequestException("Múi giờ người dùng không hợp lệ");
        }
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Người dùng không tồn tại"));
    }

    private void requireFuture(Instant dueAt) {
        if (!dueAt.isAfter(Instant.now())) {
            throw new BadRequestException("Thời gian nhắc phải ở tương lai");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record ReminderListItem(CareReminder reminder, ReminderStatusFilter status) {
    }
}

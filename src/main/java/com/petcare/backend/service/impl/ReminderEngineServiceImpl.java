package com.petcare.backend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petcare.backend.dto.vaccination.response.VaccinationSafetyWarningResponse;
import com.petcare.backend.model.CareReminder;
import com.petcare.backend.model.CareReminderLog;
import com.petcare.backend.model.Notification;
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
import com.petcare.backend.service.ReminderEngineService;
import com.petcare.backend.service.VaccinationSafetyService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ReminderEngineServiceImpl implements ReminderEngineService {
    private static final ZoneId SYSTEM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final CareReminderRepository reminderRepository;
    private final CareReminderLogRepository reminderLogRepository;
    private final VaccinationReminderLogRepository vaccinationLogRepository;
    private final PetVaccinationRepository vaccinationRepository;
    private final PetCoParentRepository coParentRepository;
    private final NotificationRepository notificationRepository;
    private final ReminderScheduleCalculator scheduleCalculator;
    private final PushNotificationSender pushNotificationSender;
    private final VaccinationSafetyService vaccinationSafetyService;
    private final ObjectMapper objectMapper;

    @Value("${app.reminder.batch-size:100}")
    private int batchSize;

    @Value("${app.reminder.vaccine-notification-time:09:00}")
    private String vaccineNotificationTime;

    @Override
    @Transactional
    public void processDueCustomReminders() {
        Instant now = Instant.now();
        List<CareReminderLog> dueLogs = reminderLogRepository.findDueForUpdate(
                CareReminderLog.ReminderLogStatus.pending,
                now,
                PageRequest.of(0, batchSize)
        );

        for (CareReminderLog log : dueLogs) {
            CareReminder reminder = log.getReminder();
            Notification notification = createNotification(
                    reminder.getCreatedBy(),
                    reminder.getTitle(),
                    buildCustomBody(reminder),
                    "care_reminder",
                    Map.of(
                            "reminderId", reminder.getId(),
                            "petId", reminder.getPet().getId(),
                            "category", reminder.getCategory().name(),
                            "dueAt", log.getDueAt().toString()
                    )
            );

            log.setStatus(CareReminderLog.ReminderLogStatus.notified);
            log.setNotifiedAt(now);
            log.setSnoozedUntil(null);
            reminderLogRepository.save(log);

            Instant nextDue = scheduleCalculator.nextDue(reminder, log.getDueAt());
            if (nextDue != null && !reminderLogRepository.existsByReminderIdAndDueAt(reminder.getId(), nextDue)) {
                CareReminderLog nextLog = new CareReminderLog();
                nextLog.setReminder(reminder);
                nextLog.setDueAt(nextDue);
                nextLog.setDueDate(scheduleCalculator.toLocalDate(nextDue, reminder.getTimezone()));
                nextLog.setStatus(CareReminderLog.ReminderLogStatus.pending);
                reminderLogRepository.save(nextLog);
                reminder.setNextDueAt(nextDue);
                reminder.setNextDueDate(nextLog.getDueDate());
            } else {
                reminder.setNextDueAt(null);
            }
            reminderRepository.save(reminder);
            pushNotificationSender.send(notification);
        }
    }

    @Override
    @Transactional
    public void processSystemVaccinationReminders() {
        Instant now = Instant.now();
        LocalDate systemToday = LocalDate.now(SYSTEM_ZONE);
        markOverdueVaccinations(systemToday);

        List<PetVaccination> vaccinations = vaccinationRepository.findByStatusInAndScheduledDateBetween(
                List.of(
                        PetVaccination.VaccinationStatus.scheduled,
                        PetVaccination.VaccinationStatus.overdue
                ),
                systemToday.minusDays(15),
                systemToday.plusDays(8)
        );

        for (PetVaccination vaccination : vaccinations) {
            for (User recipient : recipients(vaccination)) {
                processVaccinationForRecipient(vaccination, recipient, now);
            }
        }
    }

    private void markOverdueVaccinations(LocalDate today) {
        vaccinationRepository.findByStatusAndScheduledDateBefore(
                PetVaccination.VaccinationStatus.scheduled,
                today
        ).forEach(vaccination -> {
            vaccination.setStatus(PetVaccination.VaccinationStatus.overdue);
            vaccinationRepository.save(vaccination);
        });
    }

    private void processVaccinationForRecipient(
            PetVaccination vaccination,
            User recipient,
            Instant now) {
        ZoneId zoneId = validZone(recipient.getTimezone());
        LocalDate recipientToday = now.atZone(zoneId).toLocalDate();
        long offset = ChronoUnit.DAYS.between(vaccination.getScheduledDate(), recipientToday);
        VaccinationReminderLog.VaccinationReminderStage stage = stageForOffset(offset);
        if (stage == null) {
            return;
        }

        LocalTime notificationTime = LocalTime.parse(vaccineNotificationTime);
        Instant scheduledAt = vaccination.getScheduledDate()
                .plusDays(stage.getDayOffset())
                .atTime(notificationTime)
                .atZone(zoneId)
                .toInstant();
        if (now.isBefore(scheduledAt)) {
            return;
        }

        VaccinationReminderLog log = vaccinationLogRepository
                .findByVaccinationIdAndUserIdAndStage(
                        vaccination.getId(), recipient.getId(), stage
                )
                .orElseGet(VaccinationReminderLog::new);
        if (log.getStatus() == VaccinationReminderLog.VaccinationReminderStatus.notified) {
            return;
        }

        log.setVaccination(vaccination);
        log.setUser(recipient);
        log.setStage(stage);
        log.setScheduledAt(scheduledAt);
        log.setStatus(VaccinationReminderLog.VaccinationReminderStatus.pending);
        vaccinationLogRepository.save(log);

        VaccinationSafetyWarningResponse safetyWarning = vaccinationSafetyService.evaluate(vaccination);
        Notification notification = createNotification(
                recipient,
                vaccinationTitle(vaccination, stage, safetyWarning),
                vaccinationBody(vaccination, stage, safetyWarning),
                "vaccination_reminder",
                vaccinationReminderData(vaccination, stage, safetyWarning)
        );
        log.setStatus(VaccinationReminderLog.VaccinationReminderStatus.notified);
        log.setNotifiedAt(now);
        vaccinationLogRepository.save(log);
        pushNotificationSender.send(notification);
    }

    private List<User> recipients(PetVaccination vaccination) {
        Map<Long, User> unique = new LinkedHashMap<>();
        User owner = vaccination.getPet().getOwner();
        unique.put(owner.getId(), owner);
        for (PetCoParent coParent : coParentRepository.findByPetId(vaccination.getPet().getId())) {
            unique.put(coParent.getUser().getId(), coParent.getUser());
        }
        return new ArrayList<>(unique.values());
    }

    private Notification createNotification(
            User user,
            String title,
            String body,
            String type,
            Map<String, ?> data) {
        LocalDateTime now = LocalDateTime.now();
        Notification notification = new Notification();
        notification.setReceiver(user);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setType(type);
        notification.setData(writeJson(data));
        notification.setStatus("sent");
        notification.setScheduledAt(now);
        notification.setSentAt(now);
        return notificationRepository.save(notification);
    }

    private String buildCustomBody(CareReminder reminder) {
        return StringUtils.hasText(reminder.getNotes())
                ? reminder.getNotes()
                : "Đã đến thời gian " + reminder.getTitle().toLowerCase() + ".";
    }

    private String vaccinationTitle(
            PetVaccination vaccination,
            VaccinationReminderLog.VaccinationReminderStage stage,
            VaccinationSafetyWarningResponse safetyWarning) {
        if (isMedicalWarningStage(stage) && safetyWarning.isWarning()) {
            return "[CẢNH BÁO Y TẾ] " + vaccination.getPet().getName() + " đến lịch tiêm";
        }
        return switch (stage) {
            case BEFORE_7_DAYS -> "Còn 7 ngày đến lịch tiêm";
            case BEFORE_1_DAY -> "Ngày mai đến lịch tiêm";
            case DUE_TODAY -> "Hôm nay đến lịch tiêm";
            case OVERDUE_1_DAY -> "Lịch tiêm đã quá hạn 1 ngày";
            case OVERDUE_3_DAYS -> "Lịch tiêm đã quá hạn 3 ngày";
            case OVERDUE_7_DAYS -> "Cảnh báo lịch tiêm quá hạn 1 tuần";
            case OVERDUE_14_DAYS -> "Cảnh báo lịch tiêm quá hạn 2 tuần";
        };
    }

    private String vaccinationBody(
            PetVaccination vaccination,
            VaccinationReminderLog.VaccinationReminderStage stage,
            VaccinationSafetyWarningResponse safetyWarning) {
        String base = "Mũi " + vaccination.getVaccineName()
                + " của bé " + vaccination.getPet().getName()
                + " có lịch ngày " + vaccination.getScheduledDate() + ".";
        if (isMedicalWarningStage(stage) && safetyWarning.isWarning()) {
            return safetyWarning.getMessage()
                    + " Lý do: " + String.join("; ", safetyWarning.getReasons().stream().limit(3).toList());
        }
        if (stage == VaccinationReminderLog.VaccinationReminderStage.OVERDUE_14_DAYS) {
            return base + " Vui lòng tham khảo bác sĩ thú y về phác đồ tiếp theo.";
        }
        return base;
    }

    private Map<String, Object> vaccinationReminderData(
            PetVaccination vaccination,
            VaccinationReminderLog.VaccinationReminderStage stage,
            VaccinationSafetyWarningResponse safetyWarning) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("petId", vaccination.getPet().getId());
        data.put("vaccinationId", vaccination.getId());
        data.put("stage", stage.name());
        data.put("scheduledDate", vaccination.getScheduledDate().toString());
        data.put("safetyWarning", safetyWarning.isWarning());
        data.put("safetyWarningLevel", safetyWarning.getWarningLevel().name());
        return data;
    }

    private boolean isMedicalWarningStage(VaccinationReminderLog.VaccinationReminderStage stage) {
        return stage == VaccinationReminderLog.VaccinationReminderStage.DUE_TODAY
                || stage == VaccinationReminderLog.VaccinationReminderStage.OVERDUE_1_DAY
                || stage == VaccinationReminderLog.VaccinationReminderStage.OVERDUE_3_DAYS
                || stage == VaccinationReminderLog.VaccinationReminderStage.OVERDUE_7_DAYS
                || stage == VaccinationReminderLog.VaccinationReminderStage.OVERDUE_14_DAYS;
    }

    private String vaccinationTitle(VaccinationReminderLog.VaccinationReminderStage stage) {
        return switch (stage) {
            case BEFORE_7_DAYS -> "Còn 7 ngày đến lịch tiêm";
            case BEFORE_1_DAY -> "Ngày mai đến lịch tiêm";
            case DUE_TODAY -> "Hôm nay đến lịch tiêm";
            case OVERDUE_1_DAY -> "Lịch tiêm đã quá hạn 1 ngày";
            case OVERDUE_3_DAYS -> "Lịch tiêm đã quá hạn 3 ngày";
            case OVERDUE_7_DAYS -> "Cảnh báo lịch tiêm quá hạn 1 tuần";
            case OVERDUE_14_DAYS -> "Cảnh báo lịch tiêm quá hạn 2 tuần";
        };
    }

    private String vaccinationBody(
            PetVaccination vaccination,
            VaccinationReminderLog.VaccinationReminderStage stage) {
        String base = "Mũi " + vaccination.getVaccineName()
                + " của bé " + vaccination.getPet().getName()
                + " có lịch ngày " + vaccination.getScheduledDate() + ".";
        if (stage == VaccinationReminderLog.VaccinationReminderStage.OVERDUE_14_DAYS) {
            return base + " Vui lòng tham khảo bác sĩ thú y về phác đồ tiếp theo.";
        }
        return base;
    }

    private VaccinationReminderLog.VaccinationReminderStage stageForOffset(long offset) {
        for (VaccinationReminderLog.VaccinationReminderStage stage
                : VaccinationReminderLog.VaccinationReminderStage.values()) {
            if (stage.getDayOffset() == offset) {
                return stage;
            }
        }
        return null;
    }

    private ZoneId validZone(String timezone) {
        try {
            return ZoneId.of(StringUtils.hasText(timezone) ? timezone : "Asia/Ho_Chi_Minh");
        } catch (Exception ex) {
            return SYSTEM_ZONE;
        }
    }

    private String writeJson(Map<String, ?> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Không thể tạo dữ liệu thông báo", ex);
        }
    }
}

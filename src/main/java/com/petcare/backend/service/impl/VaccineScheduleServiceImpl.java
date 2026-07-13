package com.petcare.backend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petcare.backend.dto.pet.request.VaccinationHistoryRequest;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.model.Notification;
import com.petcare.backend.model.Pet;
import com.petcare.backend.model.PetTimelineEvent;
import com.petcare.backend.model.PetVaccination;
import com.petcare.backend.model.User;
import com.petcare.backend.model.VaccineTemplate;
import com.petcare.backend.repository.NotificationRepository;
import com.petcare.backend.repository.PetRepository;
import com.petcare.backend.repository.PetTimelineEventRepository;
import com.petcare.backend.repository.PetVaccinationRepository;
import com.petcare.backend.repository.VaccineTemplateRepository;
import com.petcare.backend.service.VaccineScheduleService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class VaccineScheduleServiceImpl implements VaccineScheduleService {
    private static final long CATCH_UP_AGE_WEEKS = 26;

    private final VaccineTemplateRepository templateRepository;
    private final PetVaccinationRepository vaccinationRepository;
    private final PetRepository petRepository;
    private final PetTimelineEventRepository timelineEventRepository;
    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void generateProposedSchedule(Pet pet, List<VaccinationHistoryRequest> histories) {
        List<VaccineTemplate> allTemplates = templateRepository
                .findBySpeciesIdAndActiveTrueAndSeriesCodeIsNotNullOrderBySeriesCodeAscDoseNumberAsc(
                        pet.getSpecies().getId()
                );
        if (allTemplates.isEmpty()) {
            throw new BadRequestException(
                    "Chưa có phác đồ vaccine được cấu hình cho loài thú cưng này"
            );
        }

        Map<String, VaccinationHistoryRequest> historyBySeries = normalizeHistories(histories);
        Set<String> seriesCodes = allTemplates.stream()
                .map(VaccineTemplate::getSeriesCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        Set<String> unsupportedSeries = historyBySeries.keySet().stream()
                .filter(seriesCode -> !seriesCodes.contains(seriesCode))
                .collect(Collectors.toSet());
        if (!unsupportedSeries.isEmpty()) {
            throw new BadRequestException("Chuỗi vaccine không được hỗ trợ: "
                    + String.join(", ", unsupportedSeries));
        }

        LocalDate today = LocalDate.now();
        LocalDate birthReference = resolveBirthReferenceDate(pet, today);
        long ageWeeks = birthReference != null
                ? Math.max(0, ChronoUnit.WEEKS.between(birthReference, today))
                : CATCH_UP_AGE_WEEKS;
        boolean reviewRequired = birthReference == null;
        int createdCount = 0;

        for (String seriesCode : seriesCodes) {
            VaccinationHistoryRequest history = historyBySeries.get(seriesCode);
            if (history == null) {
                history = unknownHistory(seriesCode);
            }
            if (history.getStatus() == VaccinationHistoryRequest.HistoryStatus.UNKNOWN
                    || (history.getStatus() == VaccinationHistoryRequest.HistoryStatus.COMPLETE
                    && history.getLastVaccinationDate() == null)) {
                reviewRequired = true;
            }

            VaccineTemplate.TargetStage stage = resolveTargetStage(ageWeeks, history);
            List<VaccineTemplate> templates = allTemplates.stream()
                    .filter(template -> seriesCode.equals(template.getSeriesCode()))
                    .filter(template -> template.getTargetStage() == stage)
                    .filter(template -> !Boolean.TRUE.equals(template.getOptional()))
                    .toList();

            createdCount += generateSeries(pet, history, stage, templates, birthReference, today);
        }

        if (createdCount == 0) {
            throw new BadRequestException(
                    "Không thể sinh lịch tiêm từ lịch sử vaccine đã cung cấp"
            );
        } else if (reviewRequired) {
            pet.setVaccinePlanStatus(Pet.VaccinePlanStatus.REVIEW_REQUIRED);
            createConsultationNotification(pet);
        } else {
            pet.setVaccinePlanStatus(Pet.VaccinePlanStatus.PROPOSED);
        }
        petRepository.save(pet);
    }

    @Override
    @Transactional
    public List<PetVaccination> confirmPlan(Pet pet, User confirmer, String notes) {
        List<PetVaccination> proposed = vaccinationRepository
                .findByPetIdAndStatusOrderBySeriesCodeAscDoseNumberAsc(
                        pet.getId(), PetVaccination.VaccinationStatus.proposed
                );
        if (proposed.isEmpty()) {
            throw new BadRequestException("Không có lịch tiêm đề xuất để xác nhận");
        }

        LocalDateTime now = LocalDateTime.now();
        proposed.forEach(vaccination -> {
            vaccination.setStatus(PetVaccination.VaccinationStatus.scheduled);
            vaccination.setConfirmedAt(now);
            vaccination.setConfirmedBy(confirmer);
            if (StringUtils.hasText(notes)) {
                vaccination.setNotes(appendNote(vaccination.getNotes(), notes.trim()));
            }
        });
        vaccinationRepository.saveAll(proposed);
        pet.setVaccinePlanStatus(Pet.VaccinePlanStatus.ACTIVE);
        petRepository.save(pet);
        return proposed;
    }

    @Override
    @Transactional
    public void recalculateAfterCompletion(PetVaccination completedVaccination) {
        if (!StringUtils.hasText(completedVaccination.getSeriesCode())
                || completedVaccination.getActualDate() == null) {
            return;
        }

        List<PetVaccination> downstream = vaccinationRepository
                .findByPetIdAndSeriesCodeAndDoseNumberGreaterThanOrderByDoseNumberAsc(
                        completedVaccination.getPet().getId(),
                        completedVaccination.getSeriesCode(),
                        completedVaccination.getDoseNumber()
                );

        LocalDate anchor = completedVaccination.getActualDate();
        int adjusted = 0;
        for (PetVaccination vaccination : downstream) {
            if (vaccination.getStatus() == PetVaccination.VaccinationStatus.completed) {
                if (vaccination.getActualDate() != null) {
                    anchor = vaccination.getActualDate();
                }
                continue;
            }
            if (vaccination.getStatus() == PetVaccination.VaccinationStatus.skipped
                    || vaccination.getStatus() == PetVaccination.VaccinationStatus.cancelled) {
                continue;
            }
            if (Boolean.TRUE.equals(vaccination.getScheduleLocked())) {
                anchor = vaccination.getScheduledDate();
                continue;
            }

            LocalDate recalculated = anchor.plusDays(defaultZero(vaccination.getIntervalFromPreviousDays()));
            LocalDate minimumDate = minimumDate(completedVaccination.getPet(), vaccination.getMinimumAgeWeeks());
            if (minimumDate != null && minimumDate.isAfter(recalculated)) {
                recalculated = minimumDate;
            }
            if (!Objects.equals(vaccination.getScheduledDate(), recalculated)) {
                vaccination.setScheduledDate(recalculated);
                if (vaccination.getStatus() == PetVaccination.VaccinationStatus.overdue) {
                    vaccination.setStatus(PetVaccination.VaccinationStatus.scheduled);
                }
                vaccinationRepository.save(vaccination);
                adjusted++;
            }
            anchor = recalculated;
        }

        if (adjusted > 0) {
            createScheduleAdjustedTimeline(completedVaccination, adjusted);
        }
        createNextBoosterIfNeeded(completedVaccination, downstream);
    }

    private int generateSeries(
            Pet pet,
            VaccinationHistoryRequest history,
            VaccineTemplate.TargetStage stage,
            List<VaccineTemplate> templates,
            LocalDate birthReference,
            LocalDate today) {
        if (templates.isEmpty()) {
            return 0;
        }

        int completedDoses = stage == VaccineTemplate.TargetStage.ADULT
                ? 0
                : history.getCompletedDoses() == null ? 0 : history.getCompletedDoses();
        LocalDate previousDate = history.getLastVaccinationDate();
        int created = 0;

        for (VaccineTemplate template : templates) {
            if (template.getDoseNumber() <= completedDoses) {
                continue;
            }

            LocalDate scheduledDate = calculateScheduledDate(
                    template, history, birthReference, previousDate, today
            );
            PetVaccination vaccination = fromTemplate(pet, template, scheduledDate);
            vaccinationRepository.save(vaccination);
            previousDate = scheduledDate;
            created++;
        }
        return created;
    }

    private LocalDate calculateScheduledDate(
            VaccineTemplate template,
            VaccinationHistoryRequest history,
            LocalDate birthReference,
            LocalDate previousDate,
            LocalDate today) {
        if (template.getTargetStage() == VaccineTemplate.TargetStage.ADULT
                && history.getLastVaccinationDate() != null
                && template.getBoosterIntervalMonths() != null
                && template.getBoosterIntervalMonths() > 0) {
            return latest(today, history.getLastVaccinationDate().plusMonths(template.getBoosterIntervalMonths()));
        }

        LocalDate minimumDate = birthReference == null
                ? today
                : birthReference.plusWeeks(template.effectiveMinimumAgeWeeks());
        LocalDate intervalDate = previousDate != null && defaultZero(template.getIntervalFromPreviousDays()) > 0
                ? previousDate.plusDays(template.getIntervalFromPreviousDays())
                : today;
        return latest(today, minimumDate, intervalDate);
    }

    private PetVaccination fromTemplate(Pet pet, VaccineTemplate template, LocalDate scheduledDate) {
        PetVaccination vaccination = new PetVaccination();
        vaccination.setPet(pet);
        vaccination.setVaccineTemplate(template);
        vaccination.setVaccineName(template.getVaccineName());
        vaccination.setSeriesCode(template.getSeriesCode());
        vaccination.setTargetStage(template.getTargetStage());
        vaccination.setDoseNumber(template.getDoseNumber());
        vaccination.setMinimumAgeWeeks(template.effectiveMinimumAgeWeeks());
        vaccination.setIntervalFromPreviousDays(defaultZero(template.getIntervalFromPreviousDays()));
        vaccination.setBoosterIntervalMonths(template.getBoosterIntervalMonths());
        vaccination.setScheduleSource(PetVaccination.ScheduleSource.AUTO_TEMPLATE);
        vaccination.setScheduleLocked(false);
        vaccination.setStatus(PetVaccination.VaccinationStatus.proposed);
        vaccination.setScheduledDate(scheduledDate);
        return vaccination;
    }

    private void createNextBoosterIfNeeded(
            PetVaccination completed,
            List<PetVaccination> downstream) {
        boolean hasPendingDownstream = downstream.stream().anyMatch(vaccination ->
                vaccination.getStatus() == PetVaccination.VaccinationStatus.proposed
                        || vaccination.getStatus() == PetVaccination.VaccinationStatus.scheduled
                        || vaccination.getStatus() == PetVaccination.VaccinationStatus.overdue
        );
        if (hasPendingDownstream) {
            return;
        }

        VaccineTemplate adultTemplate = templateRepository
                .findFirstBySpeciesIdAndSeriesCodeAndTargetStageAndActiveTrue(
                        completed.getPet().getSpecies().getId(),
                        completed.getSeriesCode(),
                        VaccineTemplate.TargetStage.ADULT
                ).orElse(null);
        Integer intervalMonths = completed.getBoosterIntervalMonths() != null
                ? completed.getBoosterIntervalMonths()
                : adultTemplate != null ? adultTemplate.getBoosterIntervalMonths() : null;
        if (intervalMonths == null || intervalMonths <= 0) {
            return;
        }

        LocalDate nextDate = completed.getActualDate().plusMonths(intervalMonths);
        if (vaccinationRepository.existsByPetIdAndSeriesCodeAndScheduledDateAndStatusNot(
                completed.getPet().getId(), completed.getSeriesCode(), nextDate,
                PetVaccination.VaccinationStatus.cancelled)) {
            return;
        }

        PetVaccination booster = adultTemplate != null
                ? fromTemplate(completed.getPet(), adultTemplate, nextDate)
                : copyAsBooster(completed, nextDate);
        booster.setDoseNumber(completed.getDoseNumber() + 1);
        booster.setStatus(completed.getPet().getVaccinePlanStatus() == Pet.VaccinePlanStatus.ACTIVE
                ? PetVaccination.VaccinationStatus.scheduled
                : PetVaccination.VaccinationStatus.proposed);
        vaccinationRepository.save(booster);
    }

    private PetVaccination copyAsBooster(PetVaccination completed, LocalDate nextDate) {
        PetVaccination booster = new PetVaccination();
        booster.setPet(completed.getPet());
        booster.setVaccineTemplate(completed.getVaccineTemplate());
        booster.setVaccineName(completed.getVaccineName());
        booster.setSeriesCode(completed.getSeriesCode());
        booster.setTargetStage(VaccineTemplate.TargetStage.ADULT);
        booster.setMinimumAgeWeeks(completed.getMinimumAgeWeeks());
        booster.setIntervalFromPreviousDays(0);
        booster.setBoosterIntervalMonths(completed.getBoosterIntervalMonths());
        booster.setScheduleSource(PetVaccination.ScheduleSource.AUTO_TEMPLATE);
        booster.setScheduleLocked(false);
        booster.setScheduledDate(nextDate);
        return booster;
    }

    private VaccineTemplate.TargetStage resolveTargetStage(
            long ageWeeks,
            VaccinationHistoryRequest history) {
        if (history.getStatus() == VaccinationHistoryRequest.HistoryStatus.COMPLETE) {
            return VaccineTemplate.TargetStage.ADULT;
        }
        if (ageWeeks >= CATCH_UP_AGE_WEEKS) {
            return VaccineTemplate.TargetStage.CATCH_UP;
        }
        return VaccineTemplate.TargetStage.PUPPY;
    }

    private Map<String, VaccinationHistoryRequest> normalizeHistories(
            List<VaccinationHistoryRequest> histories) {
        if (histories == null) {
            return Map.of();
        }
        try {
            return histories.stream().collect(Collectors.toMap(
                    history -> history.getSeriesCode().trim().toUpperCase(),
                    Function.identity(),
                    (first, duplicate) -> {
                        throw new BadRequestException("Mỗi chuỗi vaccine chỉ được khai báo một lần");
                    },
                    LinkedHashMap::new
            ));
        } catch (IllegalStateException ex) {
            throw new BadRequestException("Mỗi chuỗi vaccine chỉ được khai báo một lần");
        }
    }

    private VaccinationHistoryRequest unknownHistory(String seriesCode) {
        VaccinationHistoryRequest history = new VaccinationHistoryRequest();
        history.setSeriesCode(seriesCode);
        history.setStatus(VaccinationHistoryRequest.HistoryStatus.UNKNOWN);
        return history;
    }

    private LocalDate resolveBirthReferenceDate(Pet pet, LocalDate today) {
        if (pet.getDateOfBirth() != null) {
            return pet.getDateOfBirth();
        }
        if (pet.getEstimatedAgeMonths() != null) {
            return today.minusMonths(pet.getEstimatedAgeMonths());
        }
        return null;
    }

    private LocalDate minimumDate(Pet pet, Integer minimumAgeWeeks) {
        if (minimumAgeWeeks == null) {
            return null;
        }
        LocalDate birthReference = resolveBirthReferenceDate(pet, LocalDate.now());
        return birthReference != null ? birthReference.plusWeeks(minimumAgeWeeks) : null;
    }

    private void createConsultationNotification(Pet pet) {
        Notification notification = new Notification();
        notification.setReceiver(pet.getOwner());
        notification.setTitle("Cần xác nhận phác đồ vaccine");
        notification.setBody("Lịch sử tiêm của bé " + pet.getName()
                + " chưa đầy đủ. Vui lòng tham khảo bác sĩ thú y trước khi kích hoạt lịch đề xuất.");
        notification.setType("vaccination_consultation");
        notification.setStatus("pending");
        notification.setScheduledAt(LocalDateTime.now());
        try {
            notification.setData(objectMapper.writeValueAsString(Map.of(
                    "petId", pet.getId(),
                    "petName", pet.getName(),
                    "vaccinePlanStatus", Pet.VaccinePlanStatus.REVIEW_REQUIRED.name()
            )));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Không thể tạo dữ liệu thông báo vaccine", ex);
        }
        notificationRepository.save(notification);
    }

    private void createScheduleAdjustedTimeline(PetVaccination completed, int adjustedCount) {
        PetTimelineEvent event = new PetTimelineEvent();
        event.setPet(completed.getPet());
        event.setEventType(PetTimelineEvent.EventType.vaccination_schedule_adjusted);
        event.setReferenceId(completed.getId());
        event.setEventDate(completed.getActualDate());
        event.setSummary("Đã điều chỉnh " + adjustedCount + " mũi tiếp theo của chuỗi "
                + completed.getSeriesCode() + " theo ngày tiêm thực tế.");
        timelineEventRepository.save(event);
    }

    private String appendNote(String current, String addition) {
        return StringUtils.hasText(current) ? current + System.lineSeparator() + addition : addition;
    }

    private LocalDate latest(LocalDate... dates) {
        return java.util.Arrays.stream(dates)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now());
    }

    private int defaultZero(Integer value) {
        return value == null ? 0 : value;
    }
}

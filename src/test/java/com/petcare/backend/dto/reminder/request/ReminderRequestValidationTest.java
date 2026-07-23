package com.petcare.backend.dto.reminder.request;

import static org.assertj.core.api.Assertions.assertThat;

import com.petcare.backend.model.CareReminder;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class ReminderRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void createReminderRequest_validatesVaccinationConfigurationAndDateRange() {
        CreateReminderRequest request = new CreateReminderRequest();
        request.setPetId(1L);
        request.setCategory(CareReminder.ReminderCategory.vaccination);
        request.setVaccinationId(8L);
        request.setDate(LocalDate.now());
        request.setTime(LocalTime.NOON);
        request.setRepeat(CareReminder.ReminderFrequency.once);
        request.setEndDate(LocalDate.now());
        request.setNotes("Nhắc tiêm mũi tiếp theo");

        assertThat(validator.validate(request)).isEmpty();
        assertThat(request.isVaccinationConfigurationValid()).isTrue();
        assertThat(request.isEndDateValid()).isTrue();

        request.setRepeat(CareReminder.ReminderFrequency.monthly);
        assertThat(request.isVaccinationConfigurationValid()).isFalse();

        request.setCategory(CareReminder.ReminderCategory.bathing);
        assertThat(request.isVaccinationConfigurationValid()).isFalse();
        request.setVaccinationId(null);
        assertThat(request.isVaccinationConfigurationValid()).isTrue();

        request.setCategory(null);
        assertThat(request.isVaccinationConfigurationValid()).isTrue();
        request.setDate(LocalDate.now());
        request.setEndDate(LocalDate.now().minusDays(1));
        assertThat(request.isEndDateValid()).isFalse();
        request.setDate(null);
        assertThat(request.isEndDateValid()).isTrue();
    }

    @Test
    void createReminderRequest_rejectsMissingAndOutOfBoundaryValues() {
        CreateReminderRequest request = new CreateReminderRequest();
        request.setPetId(null);
        request.setCategory(CareReminder.ReminderCategory.vaccination);
        request.setVaccinationId(null);
        request.setDate(LocalDate.now().minusDays(1));
        request.setTime(null);
        request.setRepeat(null);
        request.setNotes("x".repeat(1001));

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("petId", "date", "time", "repeat", "notes", "vaccinationConfigurationValid");
    }

    @Test
    void updateAndRescheduleRequests_validateRangeRequiredFieldsAndBoundaries() {
        UpdateReminderRequest update = new UpdateReminderRequest();
        update.setDate(LocalDate.now());
        update.setTime(LocalTime.MIDNIGHT);
        update.setRepeat(CareReminder.ReminderFrequency.daily);
        update.setEndDate(LocalDate.now());
        update.setNotes("x".repeat(1000));
        update.setActive(Boolean.FALSE);

        assertThat(validator.validate(update)).isEmpty();
        assertThat(update.isDateRangeValid()).isTrue();
        assertThat(update.getActive()).isFalse();
        update.setEndDate(LocalDate.now().minusDays(1));
        assertThat(update.isDateRangeValid()).isFalse();
        update.setDate(null);
        assertThat(update.isDateRangeValid()).isTrue();
        update.setDate(LocalDate.now().minusDays(1));
        update.setNotes("x".repeat(1001));
        assertThat(validator.validate(update))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("date", "notes");

        RescheduleReminderRequest reschedule = new RescheduleReminderRequest();
        reschedule.setDate(LocalDate.now());
        reschedule.setTime(LocalTime.NOON);
        reschedule.setRepeat(CareReminder.ReminderFrequency.weekly);
        reschedule.setEndDate(LocalDate.now().plusDays(1));
        assertThat(validator.validate(reschedule)).isEmpty();
        assertThat(reschedule.isDateRangeValid()).isTrue();
        reschedule.setEndDate(LocalDate.now().minusDays(1));
        assertThat(reschedule.isDateRangeValid()).isFalse();
        reschedule.setDate(null);
        reschedule.setEndDate(null);
        reschedule.setTime(null);
        reschedule.setRepeat(null);
        assertThat(validator.validate(reschedule))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("date", "time", "repeat");
    }

    @Test
    void snoozeReminderRequest_validatesFutureInstantAndPreservesValue() {
        SnoozeReminderRequest request = new SnoozeReminderRequest();
        Instant future = Instant.now().plusSeconds(60);
        request.setSnoozedUntil(future);

        assertThat(validator.validate(request)).isEmpty();
        assertThat(request.getSnoozedUntil()).isEqualTo(future);

        request.setSnoozedUntil(Instant.now().minusSeconds(1));
        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("snoozedUntil");
        request.setSnoozedUntil(null);
        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("snoozedUntil");
    }
}

package com.petcare.backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.petcare.backend.model.CareReminder;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class ReminderScheduleCalculatorTest {
    private final ReminderScheduleCalculator calculator = new ReminderScheduleCalculator();

    @Test
    void monthlyReminderKeepsOriginalDayAndUsesLastDayWhenNeeded() {
        CareReminder reminder = reminder(
                LocalDate.of(2026, 1, 31),
                CareReminder.ReminderFrequency.monthly
        );
        Instant january = calculator.toInstant(
                LocalDate.of(2026, 1, 31),
                LocalTime.of(9, 0),
                reminder.getTimezone()
        );

        Instant february = calculator.nextDue(reminder, january);
        Instant march = calculator.nextDue(reminder, february);

        assertThat(calculator.toLocalDate(february, reminder.getTimezone()))
                .isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(calculator.toLocalDate(march, reminder.getTimezone()))
                .isEqualTo(LocalDate.of(2026, 3, 31));
    }

    @Test
    void quarterlyReminderAddsThreeMonths() {
        CareReminder reminder = reminder(
                LocalDate.of(2026, 1, 31),
                CareReminder.ReminderFrequency.quarterly
        );
        Instant current = calculator.toInstant(
                reminder.getStartDate(),
                reminder.getReminderTime(),
                reminder.getTimezone()
        );

        Instant next = calculator.nextDue(reminder, current);

        assertThat(calculator.toLocalDate(next, reminder.getTimezone()))
                .isEqualTo(LocalDate.of(2026, 4, 30));
    }

    private CareReminder reminder(LocalDate startDate, CareReminder.ReminderFrequency frequency) {
        CareReminder reminder = new CareReminder();
        reminder.setStartDate(startDate);
        reminder.setReminderTime(LocalTime.of(9, 0));
        reminder.setTimezone("Asia/Ho_Chi_Minh");
        reminder.setFrequency(frequency);
        return reminder;
    }
}

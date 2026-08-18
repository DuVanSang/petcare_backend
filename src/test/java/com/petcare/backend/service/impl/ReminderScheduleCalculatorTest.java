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

    @Test
    void yearlyReminderAddsTwelveMonthsAndUsesLastDayWhenNeeded() {
        CareReminder reminder = reminder(
                LocalDate.of(2024, 2, 29),
                CareReminder.ReminderFrequency.yearly
        );
        Instant current = calculator.toInstant(
                reminder.getStartDate(),
                reminder.getReminderTime(),
                reminder.getTimezone()
        );

        Instant next = calculator.nextDue(reminder, current);

        assertThat(calculator.toLocalDate(next, reminder.getTimezone()))
                .isEqualTo(LocalDate.of(2025, 2, 28));
    }

    // EP: one-time reminders have no next occurrence.
    @Test
    void onceReminder_HasNoNextDue() {
        CareReminder reminder = reminder(LocalDate.of(2026, 1, 1), CareReminder.ReminderFrequency.once);
        assertThat(calculator.nextDue(reminder, calculator.toInstant(reminder.getStartDate(), reminder.getReminderTime(), reminder.getTimezone())))
                .isNull();
    }

    // BVA: daily and weekly frequencies advance exactly one calendar unit.
    @Test
    void dailyAndWeeklyReminders_AdvanceByExpectedBoundary() {
        Instant current = calculator.toInstant(LocalDate.of(2026, 3, 1), LocalTime.NOON, "Asia/Ho_Chi_Minh");
        CareReminder daily = reminder(LocalDate.of(2026, 3, 1), CareReminder.ReminderFrequency.daily);
        CareReminder weekly = reminder(LocalDate.of(2026, 3, 1), CareReminder.ReminderFrequency.weekly);
        assertThat(calculator.toLocalDate(calculator.nextDue(daily, current), daily.getTimezone())).isEqualTo(LocalDate.of(2026, 3, 2));
        assertThat(calculator.toLocalDate(calculator.nextDue(weekly, current), weekly.getTimezone())).isEqualTo(LocalDate.of(2026, 3, 8));
    }

    // BVA: an end date equal to next due permits it; a prior end date excludes it.
    @Test
    void recurringReminder_RespectsEndDateBoundary() {
        CareReminder reminder = reminder(LocalDate.of(2026, 3, 1), CareReminder.ReminderFrequency.daily);
        Instant current = calculator.toInstant(LocalDate.of(2026, 3, 1), LocalTime.of(9, 0), reminder.getTimezone());
        reminder.setEndDate(LocalDate.of(2026, 3, 2));
        assertThat(calculator.nextDue(reminder, current)).isNotNull();
        reminder.setEndDate(LocalDate.of(2026, 3, 1));
        assertThat(calculator.nextDue(reminder, current)).isNull();
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

package com.petcare.backend.service.impl;

import com.petcare.backend.model.CareReminder;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
public class ReminderScheduleCalculator {
    public Instant toInstant(LocalDate date, LocalTime time, String timezone) {
        return date.atTime(time).atZone(ZoneId.of(timezone)).toInstant();
    }

    public LocalDate toLocalDate(Instant instant, String timezone) {
        return instant.atZone(ZoneId.of(timezone)).toLocalDate();
    }

    public Instant nextDue(CareReminder reminder, Instant currentDue) {
        if (reminder.getFrequency() == CareReminder.ReminderFrequency.once) {
            return null;
        }

        ZoneId zoneId = ZoneId.of(reminder.getTimezone());
        LocalDate currentDate = currentDue.atZone(zoneId).toLocalDate();
        LocalDate nextDate = switch (reminder.getFrequency()) {
            case daily -> currentDate.plusDays(1);
            case weekly -> currentDate.plusWeeks(1);
            case monthly -> nextMonthDate(currentDate, reminder.getStartDate().getDayOfMonth(), 1);
            case quarterly -> nextMonthDate(currentDate, reminder.getStartDate().getDayOfMonth(), 3);
            case yearly -> nextMonthDate(currentDate, reminder.getStartDate().getDayOfMonth(), 12);
            case once -> throw new IllegalStateException("Reminder once không có chu kỳ tiếp theo");
        };

        if (reminder.getEndDate() != null && nextDate.isAfter(reminder.getEndDate())) {
            return null;
        }
        return toInstant(nextDate, reminder.getReminderTime(), reminder.getTimezone());
    }

    private LocalDate nextMonthDate(LocalDate currentDate, int anchorDay, int months) {
        YearMonth targetMonth = YearMonth.from(currentDate).plusMonths(months);
        return targetMonth.atDay(Math.min(anchorDay, targetMonth.lengthOfMonth()));
    }
}

package com.petcare.backend.dto.reminder.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.petcare.backend.model.CareReminder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RescheduleReminderRequest {
    @NotNull(message = "Ngày nhắc không được để trống")
    @FutureOrPresent(message = "Ngày nhắc không được ở quá khứ")
    private LocalDate date;

    @NotNull(message = "Giờ nhắc không được để trống")
    private LocalTime time;

    @NotNull(message = "Tần suất nhắc không được để trống")
    private CareReminder.ReminderFrequency repeat;

    @FutureOrPresent(message = "Ngày kết thúc không được ở quá khứ")
    private LocalDate endDate;

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "Ngày kết thúc phải từ ngày bắt đầu trở đi")
    public boolean isDateRangeValid() {
        return date == null || endDate == null || !endDate.isBefore(date);
    }
}

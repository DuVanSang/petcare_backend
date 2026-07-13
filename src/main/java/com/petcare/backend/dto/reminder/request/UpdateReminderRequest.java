package com.petcare.backend.dto.reminder.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.petcare.backend.model.CareReminder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateReminderRequest {
    @FutureOrPresent(message = "Ngày nhắc không được ở quá khứ")
    private LocalDate date;

    private LocalTime time;

    private CareReminder.ReminderFrequency repeat;

    @FutureOrPresent(message = "Ngày kết thúc không được ở quá khứ")
    private LocalDate endDate;

    @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
    private String notes;

    private Boolean active;

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "Ngày kết thúc phải từ ngày bắt đầu trở đi")
    public boolean isDateRangeValid() {
        return date == null || endDate == null || !endDate.isBefore(date);
    }
}

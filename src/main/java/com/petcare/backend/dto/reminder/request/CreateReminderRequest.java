package com.petcare.backend.dto.reminder.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.petcare.backend.model.CareReminder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReminderRequest {
    @NotNull(message = "Vui lòng chọn thú cưng")
    private Long petId;

    @NotNull(message = "Vui lòng chọn loại nhắc nhở")
    private CareReminder.ReminderCategory category;

    private Long vaccinationId;

    @NotNull(message = "Ngày nhắc không được để trống")
    @FutureOrPresent(message = "Ngày nhắc không được ở quá khứ")
    private LocalDate date;

    @NotNull(message = "Giờ nhắc không được để trống")
    private LocalTime time;

    @NotNull(message = "Tần suất nhắc không được để trống")
    private CareReminder.ReminderFrequency repeat;

    @FutureOrPresent(message = "Ngày kết thúc không được ở quá khứ")
    private LocalDate endDate;

    @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
    private String notes;

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "Thông tin nhắc vaccine không hợp lệ")
    public boolean isVaccinationConfigurationValid() {
        if (category == null) {
            return true;
        }
        if (category == CareReminder.ReminderCategory.vaccination) {
            return vaccinationId != null && repeat == CareReminder.ReminderFrequency.once;
        }
        return vaccinationId == null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "Ngày kết thúc phải từ ngày bắt đầu trở đi")
    public boolean isEndDateValid() {
        return date == null || endDate == null || !endDate.isBefore(date);
    }
}

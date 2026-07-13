package com.petcare.backend.dto.reminder.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SnoozeReminderRequest {
    @NotNull(message = "Thời gian báo lại không được để trống")
    @Future(message = "Thời gian báo lại phải ở tương lai")
    private Instant snoozedUntil;
}

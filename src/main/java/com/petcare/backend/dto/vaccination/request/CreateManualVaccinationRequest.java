package com.petcare.backend.dto.vaccination.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateManualVaccinationRequest {
    @NotNull(message = "Vaccine không được để trống")
    private Long vaccineTemplateId;

    @NotNull(message = "Ngày tiêm dự kiến không được để trống")
    @FutureOrPresent(message = "Ngày tiêm dự kiến không được ở quá khứ")
    private LocalDate scheduledDate;

    @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
    private String notes;
}

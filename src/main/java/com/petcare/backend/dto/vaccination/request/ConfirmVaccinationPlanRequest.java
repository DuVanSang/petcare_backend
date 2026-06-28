package com.petcare.backend.dto.vaccination.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfirmVaccinationPlanRequest {
    @NotNull(message = "Xác nhận tham khảo bác sĩ không được để trống")
    @AssertTrue(message = "Bạn cần xác nhận đã tham khảo bác sĩ thú y")
    private Boolean veterinarianConsulted;

    @Size(max = 1000, message = "Ghi chú xác nhận không được vượt quá 1000 ký tự")
    private String notes;
}

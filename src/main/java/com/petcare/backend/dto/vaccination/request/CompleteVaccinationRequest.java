package com.petcare.backend.dto.vaccination.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompleteVaccinationRequest {
    @NotNull(message = "Ngày tiêm thực tế không được để trống")
    @PastOrPresent(message = "Ngày tiêm thực tế không được ở tương lai")
    private LocalDate actualDate;

    @Size(max = 150, message = "Tên người tiêm không được vượt quá 150 ký tự")
    private String administeredBy;

    @Size(max = 150, message = "Tên phòng khám không được vượt quá 150 ký tự")
    private String clinicName;

    @DecimalMin(value = "0.00", message = "Chi phí tiêm không được âm")
    private BigDecimal cost;

    @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
    private String notes;

    @Size(max = 500, message = "URL chứng từ y tế không được vượt quá 500 ký tự")
    private String medicalProofUrl;
}

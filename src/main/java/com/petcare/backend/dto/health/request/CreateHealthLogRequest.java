package com.petcare.backend.dto.health.request;

import com.petcare.backend.model.HealthLog;
import io.swagger.v3.oas.annotations.media.Schema;
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
public class CreateHealthLogRequest {

    @NotNull(message = "Vui lòng chọn thú cưng")
    @Schema(description = "ID thú cưng", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long petId;

    @NotNull(message = "Ngày ghi nhận không được để trống")
    @PastOrPresent(message = "Ngày ghi nhận không được ở tương lai")
    @Schema(description = "Ngày ghi nhận sức khỏe", example = "2026-07-05", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate date;

    @NotNull(message = "Cân nặng không được để trống")
    @DecimalMin(value = "0.01", message = "Cân nặng phải lớn hơn 0")
    @Schema(description = "Cân nặng hiện tại (kg)", example = "4.2", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal weight;

    @NotNull(message = "Vui lòng chọn mức độ ăn uống")
    @Schema(description = "Mức độ ăn uống: good, normal, poor", example = "good", requiredMode = Schema.RequiredMode.REQUIRED)
    private HealthLog.Appetite appetite;

    @NotNull(message = "Vui lòng chọn mức độ hoạt động")
    @Schema(
            description = "Mức độ hoạt động: very_active, active, moderate, low",
            example = "active",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private HealthLog.ActivityLevel activityLevel;

    @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
    @Schema(description = "Ghi chú biểu hiện bất thường hoặc thức ăn sử dụng")
    private String notes;
}

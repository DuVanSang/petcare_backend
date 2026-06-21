package com.petcare.backend.dto.health.request;

import com.petcare.backend.model.HealthLog;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateHealthLogRequest {

    @NotNull(message = "Vui lòng chọn thú cưng")
    @Schema(description = "ID thú cưng", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long petId;

    @NotNull(message = "Cân nặng không được để trống")
    @DecimalMin(value = "0.01", message = "Cân nặng phải lớn hơn 0")
    @Schema(description = "Cân nặng hiện tại (kg)", example = "4.2", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal weight;

    @NotNull(message = "Vui lòng chọn mức độ ăn uống")
    @Schema(description = "Mức độ ăn uống: good, normal, poor", example = "good", requiredMode = Schema.RequiredMode.REQUIRED)
    private HealthLog.Appetite appetite;

    @Schema(description = "Ghi chú biểu hiện bất thường hoặc thức ăn sử dụng")
    private String notes;
}

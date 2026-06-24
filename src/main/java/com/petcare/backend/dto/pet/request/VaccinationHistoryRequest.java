package com.petcare.backend.dto.pet.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VaccinationHistoryRequest {
    @NotBlank(message = "Mã chuỗi vaccine không được để trống")
    @Pattern(regexp = "^[A-Z0-9_]{3,50}$", message = "Mã chuỗi vaccine không hợp lệ")
    private String seriesCode;

    @NotNull(message = "Trạng thái lịch sử tiêm không được để trống")
    private HistoryStatus status;

    @Min(value = 0, message = "Số mũi đã hoàn thành không được âm")
    private Integer completedDoses;

    @PastOrPresent(message = "Ngày tiêm gần nhất không được ở tương lai")
    private LocalDate lastVaccinationDate;

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "Thông tin lịch sử tiêm không nhất quán")
    public boolean isConsistent() {
        if (status == null) {
            return true;
        }
        return switch (status) {
            case NONE -> (completedDoses == null || completedDoses == 0) && lastVaccinationDate == null;
            case PARTIAL -> completedDoses != null && completedDoses > 0 && lastVaccinationDate != null;
            case COMPLETE -> completedDoses == null || completedDoses > 0;
            case UNKNOWN -> true;
        };
    }

    public enum HistoryStatus {
        NONE, PARTIAL, COMPLETE, UNKNOWN
    }
}

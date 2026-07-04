package com.petcare.backend.dto.admin.vaccine.request;

import com.petcare.backend.model.VaccineTemplate;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUpdateVaccineTemplateRequest {
    private Long speciesId;

    @Size(max = 150, message = "Tên vaccine không được quá 150 ký tự")
    private String vaccineName;

    @Size(max = 50, message = "Mã series không được quá 50 ký tự")
    private String seriesCode;

    private VaccineTemplate.TargetStage targetStage;

    @Min(value = 1, message = "Số mũi phải lớn hơn hoặc bằng 1")
    private Integer doseNumber;

    @Min(value = 0, message = "Tuổi tối thiểu không được âm")
    private Integer minimumAgeWeeks;

    @Min(value = 0, message = "Khoảng cách với mũi trước không được âm")
    private Integer intervalFromPreviousDays;

    @Min(value = 1, message = "Chu kỳ nhắc lại phải lớn hơn hoặc bằng 1 tháng")
    private Integer boosterIntervalMonths;

    private Boolean optional;

    private Boolean active;

    @Size(max = 2000, message = "Mô tả không được quá 2000 ký tự")
    private String description;
}

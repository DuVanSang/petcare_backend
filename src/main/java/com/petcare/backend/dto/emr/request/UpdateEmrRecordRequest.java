package com.petcare.backend.dto.emr.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.petcare.backend.dto.emr.EmrAttachmentDto;
import com.petcare.backend.model.EmrRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class UpdateEmrRecordRequest {

    @NotNull(message = "Vui lòng chọn loại hồ sơ")
    @JsonProperty("record_type")
    @Schema(description = "Loại hồ sơ: visit, prescription, lab_result, surgery, other", example = "visit")
    private EmrRecord.RecordType recordType;

    @NotNull(message = "Ngày khám không được để trống")
    @JsonProperty("visit_date")
    @Schema(description = "Ngày khám / ngày ghi nhận", example = "2026-06-13")
    private LocalDate visitDate;

    @Size(max = 150, message = "Tên phòng khám không được quá 150 ký tự")
    @JsonProperty("clinic_name")
    @Schema(description = "Tên phòng khám", example = "PetCare Clinic")
    private String clinicName;

    @Size(max = 150, message = "Tên bác sĩ không được quá 150 ký tự")
    @JsonProperty("vet_name")
    @Schema(description = "Tên bác sĩ thú y", example = "Dr. John Doe")
    private String vetName;

    @Size(max = 50, message = "Thông tin liên hệ không được quá 50 ký tự")
    @JsonProperty("vet_contact")
    @Schema(description = "Liên hệ bác sĩ / phòng khám", example = "0901234567")
    private String vetContact;

    @NotBlank(message = "Chẩn đoán không được để trống")
    @Schema(description = "Chẩn đoán bệnh", example = "Viêm tai ngoài dị ứng")
    private String diagnosis;

    @JsonProperty("prescription_details")
    @Schema(description = "Chi tiết đơn thuốc")
    private String prescriptionDetails;

    @Schema(description = "Ghi chú bổ sung")
    private String notes;

    @Valid
    @Schema(description = "Danh sách tài liệu đính kèm đã upload lên cloud")
    private List<EmrAttachmentDto> attachments;
}

package com.petcare.backend.dto.emr.response;

import com.petcare.backend.dto.emr.EmrAttachmentDto;
import com.petcare.backend.model.EmrRecord;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class EmrRecordResponse {

    private Long id;
    private Long petId;
    private EmrRecord.RecordType recordType;
    private LocalDate visitDate;
    private String clinicName;
    private String vetName;
    private String vetContact;
    private String diagnosis;
    private String prescriptionDetails;
    private String notes;
    private List<EmrAttachmentDto> attachments;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EmrRecordResponse from(EmrRecord record, List<EmrAttachmentDto> attachments) {
        return EmrRecordResponse.builder()
                .id(record.getId())
                .petId(record.getPet().getId())
                .recordType(record.getRecordType())
                .visitDate(record.getVisitDate())
                .clinicName(record.getClinicName())
                .vetName(record.getVetName())
                .vetContact(record.getVetContact())
                .diagnosis(record.getDiagnosis())
                .prescriptionDetails(record.getPrescriptionDetails())
                .notes(record.getNotes())
                .attachments(attachments != null ? attachments : List.of())
                .createdBy(record.getCreatedBy().getId())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }
}

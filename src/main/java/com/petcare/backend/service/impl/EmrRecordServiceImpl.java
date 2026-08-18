package com.petcare.backend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petcare.backend.dto.emr.EmrAttachmentDto;
import com.petcare.backend.dto.emr.request.CreateEmrRecordRequest;
import com.petcare.backend.dto.emr.request.UpdateEmrRecordRequest;
import com.petcare.backend.dto.emr.response.EmrRecordResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.model.EmrRecord;
import com.petcare.backend.model.Pet;
import com.petcare.backend.model.PetTimelineEvent;
import com.petcare.backend.model.User;
import com.petcare.backend.repository.EmrRecordRepository;
import com.petcare.backend.repository.PetCoParentRepository;
import com.petcare.backend.repository.PetRepository;
import com.petcare.backend.repository.PetTimelineEventRepository;
import com.petcare.backend.repository.UserRepository;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.EmrRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmrRecordServiceImpl implements EmrRecordService {

    private static final TypeReference<List<EmrAttachmentDto>> ATTACHMENT_LIST_TYPE = new TypeReference<>() {};

    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final PetCoParentRepository coParentRepository;
    private final EmrRecordRepository emrRecordRepository;
    private final PetTimelineEventRepository petTimelineEventRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public EmrRecordResponse createEmrRecord(UserPrincipal principal, CreateEmrRecordRequest request) {
        Long userId = principal.getId();
        Pet pet = getAccessiblePet(request.getPetId(), userId);
        assertCanEdit(pet, userId);

        User user = getUser(userId);
        List<EmrAttachmentDto> attachments = normalizeAttachments(request.getAttachments());

        EmrRecord record = new EmrRecord();
        record.setPet(pet);
        record.setRecordType(request.getRecordType());
        record.setVisitDate(request.getVisitDate());
        record.setClinicName(trimToNull(request.getClinicName()));
        record.setVetName(trimToNull(request.getVetName()));
        record.setVetContact(trimToNull(request.getVetContact()));
        record.setDiagnosis(request.getDiagnosis().trim());
        record.setPrescriptionDetails(trimToNull(request.getPrescriptionDetails()));
        record.setNotes(trimToNull(request.getNotes()));
        record.setAttachments(serializeAttachments(attachments));
        record.setCreatedBy(user);

        EmrRecord savedRecord = emrRecordRepository.save(record);

        PetTimelineEvent event = new PetTimelineEvent();
        event.setPet(pet);
        event.setEventType(PetTimelineEvent.EventType.medical_visit);
        event.setReferenceId(savedRecord.getId());
        event.setEventDate(request.getVisitDate());
        event.setSummary(buildTimelineSummary(pet.getName(), record.getClinicName(), record.getDiagnosis()));
        petTimelineEventRepository.save(event);

        return EmrRecordResponse.from(savedRecord, attachments);
    }

    @Override
    @Transactional
    public EmrRecordResponse updateEmrRecord(UserPrincipal principal, Long emrRecordId, UpdateEmrRecordRequest request) {
        Long userId = principal.getId();
        EmrRecord record = emrRecordRepository.findById(emrRecordId)
                .orElseThrow(() -> new BadRequestException("Hồ sơ EMR không tồn tại"));

        Pet pet = getAccessiblePet(record.getPet().getId(), userId);
        assertCanEdit(pet, userId);

        List<EmrAttachmentDto> attachments = normalizeAttachments(request.getAttachments());

        record.setRecordType(request.getRecordType());
        record.setVisitDate(request.getVisitDate());
        record.setClinicName(trimToNull(request.getClinicName()));
        record.setVetName(trimToNull(request.getVetName()));
        record.setVetContact(trimToNull(request.getVetContact()));
        record.setDiagnosis(request.getDiagnosis().trim());
        record.setPrescriptionDetails(trimToNull(request.getPrescriptionDetails()));
        record.setNotes(trimToNull(request.getNotes()));
        if (request.getAttachments() != null) {
            record.setAttachments(serializeAttachments(attachments));
        }

        EmrRecord savedRecord = emrRecordRepository.save(record);
        return EmrRecordResponse.from(savedRecord, attachments);
    }

    @Override
    @Transactional
    public void deleteEmrRecord(UserPrincipal principal, Long emrRecordId) {
        Long userId = principal.getId();
        EmrRecord record = emrRecordRepository.findById(emrRecordId)
                .orElseThrow(() -> new BadRequestException("Hồ sơ EMR không tồn tại"));

        Pet pet = getAccessiblePet(record.getPet().getId(), userId);
        assertCanEdit(pet, userId);

        emrRecordRepository.delete(record);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmrRecordResponse> getEmrRecordsByPet(UserPrincipal principal, Long petId) {
        Pet pet = getAccessiblePet(petId, principal.getId());
        return emrRecordRepository.findByPetIdOrderByVisitDateDescCreatedAtDesc(pet.getId()).stream()
                .map(record -> EmrRecordResponse.from(record, deserializeAttachments(record.getAttachments())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EmrRecordResponse getEmrRecordById(UserPrincipal principal, Long emrRecordId) {
        EmrRecord record = emrRecordRepository.findById(emrRecordId)
                .orElseThrow(() -> new BadRequestException("Hồ sơ EMR không tồn tại"));

        getAccessiblePet(record.getPet().getId(), principal.getId());
        return EmrRecordResponse.from(record, deserializeAttachments(record.getAttachments()));
    }

    private List<EmrAttachmentDto> normalizeAttachments(List<EmrAttachmentDto> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }
        return attachments;
    }

    private String serializeAttachments(List<EmrAttachmentDto> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attachments);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Không thể lưu danh sách tệp đính kèm");
        }
    }

    private List<EmrAttachmentDto> deserializeAttachments(String attachmentsJson) {
        if (!StringUtils.hasText(attachmentsJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(attachmentsJson, ATTACHMENT_LIST_TYPE);
        } catch (JsonProcessingException ex) {
            return Collections.emptyList();
        }
    }

    private String buildTimelineSummary(String petName, String clinicName, String diagnosis) {
        if (StringUtils.hasText(clinicName)) {
            return "Bé " + petName + " được khám bệnh tại " + clinicName.trim() + ": " + diagnosis.trim() + ".";
        }
        return "Bé " + petName + " được khám bệnh: " + diagnosis.trim() + ".";
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private Pet getAccessiblePet(Long petId, Long userId) {
        return petRepository.findByIdAndAccessibleByUserId(petId, userId)
                .orElseThrow(() -> new BadRequestException("Thú cưng không tồn tại hoặc bạn không có quyền truy cập"));
    }

    private void assertCanEdit(Pet pet, Long userId) {
        if ("viewer".equals(resolveRole(pet, userId))) {
            throw new BadRequestException("Bạn không có quyền tạo hồ sơ EMR cho thú cưng này");
        }
    }

    private String resolveRole(Pet pet, Long userId) {
        if (pet.getOwner().getId().equals(userId)) {
            return "owner";
        }
        return coParentRepository.findByPetIdAndUserId(pet.getId(), userId)
                .map(cp -> cp.getRole().name())
                .orElse("viewer");
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Người dùng không tồn tại"));
    }
}

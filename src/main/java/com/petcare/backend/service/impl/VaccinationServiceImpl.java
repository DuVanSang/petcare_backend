package com.petcare.backend.service.impl;

import com.petcare.backend.dto.vaccination.request.CompleteVaccinationRequest;
import com.petcare.backend.dto.vaccination.request.RescheduleVaccinationRequest;
import com.petcare.backend.dto.vaccination.request.SkipVaccinationRequest;
import com.petcare.backend.dto.vaccination.response.VaccinationResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.model.Pet;
import com.petcare.backend.model.PetCoParent;
import com.petcare.backend.model.PetTimelineEvent;
import com.petcare.backend.model.PetVaccination;
import com.petcare.backend.repository.PetCoParentRepository;
import com.petcare.backend.repository.PetRepository;
import com.petcare.backend.repository.PetTimelineEventRepository;
import com.petcare.backend.repository.PetVaccinationRepository;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.VaccinationService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class VaccinationServiceImpl implements VaccinationService {
    private final PetRepository petRepository;
    private final PetCoParentRepository coParentRepository;
    private final PetVaccinationRepository vaccinationRepository;
    private final PetTimelineEventRepository timelineEventRepository;

    @Override
    @Transactional(readOnly = true)
    public List<VaccinationResponse> getVaccinations(
            UserPrincipal principal,
            Long petId,
            PetVaccination.VaccinationStatus status) {
        ensureCanView(principal, petId);

        List<PetVaccination> vaccinations = status == null
                ? vaccinationRepository.findByPetIdOrderByScheduledDateAsc(petId)
                : vaccinationRepository.findByPetIdAndStatusOrderByScheduledDateAsc(petId, status);

        return vaccinations.stream()
                .map(VaccinationResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VaccinationResponse getVaccination(UserPrincipal principal, Long petId, Long vaccinationId) {
        ensureCanView(principal, petId);
        return VaccinationResponse.from(getVaccinationForPet(petId, vaccinationId));
    }

    @Override
    @Transactional
    public VaccinationResponse completeVaccination(
            UserPrincipal principal,
            Long petId,
            Long vaccinationId,
            CompleteVaccinationRequest request) {
        ensureCanEdit(principal, petId);
        PetVaccination vaccination = getVaccinationForPet(petId, vaccinationId);

        if (vaccination.getStatus() == PetVaccination.VaccinationStatus.completed) {
            throw new BadRequestException("Mũi tiêm này đã được hoàn thành");
        }

        vaccination.setStatus(PetVaccination.VaccinationStatus.completed);
        vaccination.setActualDate(request.getActualDate());
        vaccination.setAdministeredBy(trimToNull(request.getAdministeredBy()));
        vaccination.setClinicName(trimToNull(request.getClinicName()));
        vaccination.setCost(request.getCost());
        vaccination.setNotes(trimToNull(request.getNotes()));
        vaccination.setMedicalProofUrl(trimToNull(request.getMedicalProofUrl()));

        PetVaccination saved = vaccinationRepository.save(vaccination);
        createVaccinatedTimelineEvent(saved);
        return VaccinationResponse.from(saved);
    }

    @Override
    @Transactional
    public VaccinationResponse skipVaccination(
            UserPrincipal principal,
            Long petId,
            Long vaccinationId,
            SkipVaccinationRequest request) {
        ensureCanEdit(principal, petId);
        PetVaccination vaccination = getVaccinationForPet(petId, vaccinationId);

        if (vaccination.getStatus() == PetVaccination.VaccinationStatus.completed) {
            throw new BadRequestException("Không thể bỏ qua mũi tiêm đã hoàn thành");
        }

        vaccination.setStatus(PetVaccination.VaccinationStatus.skipped);
        vaccination.setNotes(trimToNull(request.getNotes()));
        return VaccinationResponse.from(vaccinationRepository.save(vaccination));
    }

    @Override
    @Transactional
    public VaccinationResponse rescheduleVaccination(
            UserPrincipal principal,
            Long petId,
            Long vaccinationId,
            RescheduleVaccinationRequest request) {
        ensureCanEdit(principal, petId);
        PetVaccination vaccination = getVaccinationForPet(petId, vaccinationId);

        if (vaccination.getStatus() == PetVaccination.VaccinationStatus.completed) {
            throw new BadRequestException("Không thể dời lịch mũi tiêm đã hoàn thành");
        }

        vaccination.setScheduledDate(request.getScheduledDate());
        vaccination.setStatus(PetVaccination.VaccinationStatus.scheduled);
        vaccination.setNotes(trimToNull(request.getNotes()));
        return VaccinationResponse.from(vaccinationRepository.save(vaccination));
    }

    private Pet ensureCanView(UserPrincipal principal, Long petId) {
        return petRepository.findByIdAndAccessibleByUserId(petId, principal.getId())
                .orElseThrow(() -> new BadRequestException(
                        "Thú cưng không tồn tại hoặc bạn không có quyền truy cập"
                ));
    }

    private void ensureCanEdit(UserPrincipal principal, Long petId) {
        Pet pet = ensureCanView(principal, petId);
        if (pet.getOwner().getId().equals(principal.getId())) {
            return;
        }

        PetCoParent coParent = coParentRepository.findByPetIdAndUserId(petId, principal.getId())
                .orElseThrow(() -> new BadRequestException("Bạn không có quyền chỉnh sửa lịch tiêm của thú cưng này"));

        if (coParent.getRole() != PetCoParent.CoParentRole.editor) {
            throw new BadRequestException("Bạn không có quyền chỉnh sửa lịch tiêm của thú cưng này");
        }
    }

    private PetVaccination getVaccinationForPet(Long petId, Long vaccinationId) {
        return vaccinationRepository.findByIdAndPetId(vaccinationId, petId)
                .orElseThrow(() -> new BadRequestException("Mũi tiêm không tồn tại trong hồ sơ thú cưng này"));
    }

    private void createVaccinatedTimelineEvent(PetVaccination vaccination) {
        PetTimelineEvent event = new PetTimelineEvent();
        event.setPet(vaccination.getPet());
        event.setEventType(PetTimelineEvent.EventType.vaccinated);
        event.setReferenceId(vaccination.getId());
        event.setEventDate(vaccination.getActualDate() != null ? vaccination.getActualDate() : LocalDate.now());
        event.setSummary("Bé " + vaccination.getPet().getName()
                + " đã hoàn thành mũi tiêm " + vaccination.getVaccineName() + ".");
        timelineEventRepository.save(event);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}

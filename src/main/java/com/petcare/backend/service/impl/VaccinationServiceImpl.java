package com.petcare.backend.service.impl;

import com.petcare.backend.dto.vaccination.request.CompleteVaccinationRequest;
import com.petcare.backend.dto.vaccination.request.ConfirmVaccinationPlanRequest;
import com.petcare.backend.dto.vaccination.request.RescheduleVaccinationRequest;
import com.petcare.backend.dto.vaccination.request.SetupVaccinationPlanRequest;
import com.petcare.backend.dto.vaccination.request.SkipVaccinationRequest;
import com.petcare.backend.dto.vaccination.response.VaccinationResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.model.Pet;
import com.petcare.backend.model.PetCoParent;
import com.petcare.backend.model.PetTimelineEvent;
import com.petcare.backend.model.PetVaccination;
import com.petcare.backend.model.User;
import com.petcare.backend.repository.PetCoParentRepository;
import com.petcare.backend.repository.PetRepository;
import com.petcare.backend.repository.PetTimelineEventRepository;
import com.petcare.backend.repository.PetVaccinationRepository;
import com.petcare.backend.repository.UserRepository;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.VaccinationService;
import com.petcare.backend.service.VaccineScheduleService;
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
    private final UserRepository userRepository;
    private final VaccineScheduleService vaccineScheduleService;

    @Override
    @Transactional
    public List<VaccinationResponse> setupPlan(
            UserPrincipal principal,
            Long petId,
            SetupVaccinationPlanRequest request) {
        Pet pet = ensureCanEdit(principal, petId);
        if (pet.getVaccinePlanStatus() != Pet.VaccinePlanStatus.NOT_CONFIGURED
                || vaccinationRepository.existsByPetId(petId)) {
            throw new BadRequestException("Kế hoạch tiêm của thú cưng đã được thiết lập");
        }

        vaccineScheduleService.generateProposedSchedule(pet, request.getHistories());
        return vaccinationRepository.findByPetIdOrderByScheduledDateAsc(petId).stream()
                .map(VaccinationResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public List<VaccinationResponse> confirmPlan(
            UserPrincipal principal,
            Long petId,
            ConfirmVaccinationPlanRequest request) {
        Pet pet = ensureCanEdit(principal, petId);
        User confirmer = userRepository.findById(principal.getId())
                .orElseThrow(() -> new BadRequestException("Người dùng không tồn tại"));
        return vaccineScheduleService.confirmPlan(pet, confirmer, request.getNotes()).stream()
                .map(VaccinationResponse::from)
                .toList();
    }

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
        return vaccinations.stream().map(VaccinationResponse::from).toList();
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

        if (vaccination.getStatus() == PetVaccination.VaccinationStatus.proposed) {
            throw new BadRequestException("Lịch tiêm đề xuất phải được xác nhận trước khi hoàn thành");
        }
        if (vaccination.getStatus() == PetVaccination.VaccinationStatus.completed) {
            throw new BadRequestException("Mũi tiêm này đã được hoàn thành");
        }
        if (vaccination.getStatus() == PetVaccination.VaccinationStatus.skipped) {
            throw new BadRequestException("Không thể hoàn thành mũi tiêm đã bị bỏ qua");
        }
        if (vaccination.getStatus() == PetVaccination.VaccinationStatus.cancelled) {
            throw new BadRequestException("Không thể hoàn thành mũi tiêm đã bị hủy");
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
        vaccineScheduleService.recalculateAfterCompletion(saved);
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
        if (vaccination.getStatus() == PetVaccination.VaccinationStatus.proposed) {
            throw new BadRequestException("Lịch tiêm đề xuất phải được xác nhận trước khi đánh dấu bỏ qua");
        }
        if (vaccination.getStatus() == PetVaccination.VaccinationStatus.cancelled) {
            throw new BadRequestException("Mũi tiêm này đã bị hủy");
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
        if (vaccination.getStatus() == PetVaccination.VaccinationStatus.cancelled) {
            throw new BadRequestException("Không thể dời lịch mũi tiêm đã bị hủy");
        }
        boolean isProposed = vaccination.getStatus() == PetVaccination.VaccinationStatus.proposed;
        vaccination.setScheduledDate(request.getScheduledDate());
        vaccination.setStatus(isProposed
                ? PetVaccination.VaccinationStatus.proposed
                : PetVaccination.VaccinationStatus.scheduled);
        vaccination.setScheduleLocked(true);
        vaccination.setScheduleSource(PetVaccination.ScheduleSource.MANUAL);
        vaccination.setNotes(trimToNull(request.getNotes()));
        return VaccinationResponse.from(vaccinationRepository.save(vaccination));
    }

    private Pet ensureCanView(UserPrincipal principal, Long petId) {
        return petRepository.findByIdAndAccessibleByUserId(petId, principal.getId())
                .orElseThrow(() -> new BadRequestException(
                        "Thú cưng không tồn tại hoặc bạn không có quyền truy cập"
                ));
    }

    private Pet ensureCanEdit(UserPrincipal principal, Long petId) {
        Pet pet = ensureCanView(principal, petId);
        if (pet.getOwner().getId().equals(principal.getId())) {
            return pet;
        }
        PetCoParent coParent = coParentRepository.findByPetIdAndUserId(petId, principal.getId())
                .orElseThrow(() -> new BadRequestException(
                        "Bạn không có quyền chỉnh sửa lịch tiêm của thú cưng này"
                ));
        if (coParent.getRole() != PetCoParent.CoParentRole.editor) {
            throw new BadRequestException("Bạn không có quyền chỉnh sửa lịch tiêm của thú cưng này");
        }
        return pet;
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
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

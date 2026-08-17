package com.petcare.backend.service.impl;

import com.petcare.backend.dto.health.request.CreateHealthLogRequest;
import com.petcare.backend.dto.health.request.UpdateHealthLogRequest;
import com.petcare.backend.dto.health.response.HealthLogResponse;
import com.petcare.backend.dto.health.response.TimelineEventResponse;
import com.petcare.backend.dto.health.response.WeightLogResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.model.HealthLog;
import com.petcare.backend.model.Pet;
import com.petcare.backend.model.PetTimelineEvent;
import com.petcare.backend.model.User;
import com.petcare.backend.model.WeightLog;
import com.petcare.backend.repository.HealthLogRepository;
import com.petcare.backend.repository.PetCoParentRepository;
import com.petcare.backend.repository.PetRepository;
import com.petcare.backend.repository.PetTimelineEventRepository;
import com.petcare.backend.repository.UserRepository;
import com.petcare.backend.repository.WeightLogRepository;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.HealthLogService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class HealthLogServiceImpl implements HealthLogService {

    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final PetCoParentRepository coParentRepository;
    private final HealthLogRepository healthLogRepository;
    private final WeightLogRepository weightLogRepository;
    private final PetTimelineEventRepository petTimelineEventRepository;

    @Override
    @Transactional
    public HealthLogResponse createHealthLog(UserPrincipal principal, CreateHealthLogRequest request) {
        Long userId = principal.getId();
        Pet pet = getAccessiblePet(request.getPetId(), userId);
        assertCanEdit(pet, userId);

        User user = getUser(userId);
        LocalDate loggedDate = request.getDate();

        HealthLog healthLog = healthLogRepository.findByPetIdAndLoggedDate(pet.getId(), loggedDate)
                .orElseGet(() -> {
                    HealthLog log = new HealthLog();
                    log.setPet(pet);
                    log.setLoggedDate(loggedDate);
                    return log;
                });
        healthLog.setAppetite(request.getAppetite());
        healthLog.setActivityLevel(request.getActivityLevel());
        healthLog.setTreatmentNotes(StringUtils.hasText(request.getNotes()) ? request.getNotes().trim() : null);
        healthLog.setLoggedBy(user);
        HealthLog savedHealthLog = healthLogRepository.save(healthLog);

        WeightLog weightLog = new WeightLog();
        weightLog.setPet(pet);
        weightLog.setWeight(request.getWeight());
        weightLog.setLoggedDate(loggedDate);
        weightLog.setLoggedBy(user);
        WeightLog savedWeightLog = weightLogRepository.save(weightLog);

        pet.setCurrentWeight(request.getWeight());
        petRepository.save(pet);

        PetTimelineEvent event = new PetTimelineEvent();
        event.setPet(pet);
        event.setEventType(PetTimelineEvent.EventType.weight_updated);
        event.setReferenceId(savedWeightLog.getId());
        event.setEventDate(loggedDate);
        event.setSummary("Cân nặng được cập nhật mới: " + formatWeight(request.getWeight()) + " kg.");
        petTimelineEventRepository.save(event);

        return HealthLogResponse.from(savedHealthLog, request.getWeight(), pet.getCurrentWeight());
    }

    @Override
    @Transactional
    public HealthLogResponse updateHealthLog(UserPrincipal principal, Long logId, UpdateHealthLogRequest request) {
        Long userId = principal.getId();
        HealthLog healthLog = healthLogRepository.findById(logId)
                .orElseThrow(() -> new BadRequestException("Hồ sơ sức khỏe không tồn tại"));

        Pet pet = getAccessiblePet(healthLog.getPet().getId(), userId);
        assertCanEdit(pet, userId);

        LocalDate loggedDate = request.getDate();
        healthLog.setLoggedDate(loggedDate);
        healthLog.setAppetite(request.getAppetite());
        healthLog.setActivityLevel(request.getActivityLevel());
        healthLog.setTreatmentNotes(StringUtils.hasText(request.getNotes()) ? request.getNotes().trim() : null);
        HealthLog savedHealthLog = healthLogRepository.save(healthLog);

        if (request.getWeight() != null) {
            User user = getUser(userId);
            WeightLog weightLog = new WeightLog();
            weightLog.setPet(pet);
            weightLog.setWeight(request.getWeight());
            weightLog.setLoggedDate(loggedDate);
            weightLog.setLoggedBy(user);
            weightLogRepository.save(weightLog);

            pet.setCurrentWeight(request.getWeight());
            petRepository.save(pet);
        }

        return HealthLogResponse.from(savedHealthLog, request.getWeight(), pet.getCurrentWeight());
    }

    @Override
    @Transactional
    public void deleteHealthLog(UserPrincipal principal, Long logId) {
        Long userId = principal.getId();
        HealthLog healthLog = healthLogRepository.findById(logId)
                .orElseThrow(() -> new BadRequestException("Hồ sơ sức khỏe không tồn tại"));

        Pet pet = getAccessiblePet(healthLog.getPet().getId(), userId);
        assertCanEdit(pet, userId);

        healthLogRepository.delete(healthLog);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HealthLogResponse> getHealthLogs(UserPrincipal principal, Long petId) {
        Pet pet = getAccessiblePet(petId, principal.getId());
        Map<LocalDate, BigDecimal> weightByDate = weightLogRepository
                .findByPetIdOrderByLoggedDateAsc(pet.getId()).stream()
                .collect(Collectors.toMap(
                        WeightLog::getLoggedDate,
                        WeightLog::getWeight,
                        (existing, replacement) -> replacement
                ));
        return healthLogRepository.findByPetIdOrderByLoggedDateDesc(pet.getId()).stream()
                .map(log -> HealthLogResponse.from(
                        log,
                        weightByDate.get(log.getLoggedDate()),
                        pet.getCurrentWeight()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WeightLogResponse> getWeightLogs(UserPrincipal principal, Long petId) {
        Pet pet = getAccessiblePet(petId, principal.getId());
        return weightLogRepository.findByPetIdOrderByLoggedDateAsc(pet.getId()).stream()
                .map(WeightLogResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimelineEventResponse> getTimeline(UserPrincipal principal, Long petId) {
        Pet pet = getAccessiblePet(petId, principal.getId());
        return petTimelineEventRepository.findByPetIdOrderByEventDateDescCreatedAtDesc(pet.getId()).stream()
                .map(TimelineEventResponse::from)
                .collect(Collectors.toList());
    }

    private Pet getAccessiblePet(Long petId, Long userId) {
        return petRepository.findByIdAndAccessibleByUserId(petId, userId)
                .orElseThrow(() -> new BadRequestException(
                        "Thú cưng không tồn tại hoặc bạn không có quyền truy cập"
                ));
    }

    private void assertCanEdit(Pet pet, Long userId) {
        if ("viewer".equals(resolveRole(pet, userId))) {
            throw new BadRequestException("Bạn không có quyền ghi nhật ký sức khỏe cho thú cưng này");
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

    private String formatWeight(BigDecimal weight) {
        return weight.stripTrailingZeros().toPlainString();
    }
}

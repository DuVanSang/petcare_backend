package com.petcare.backend.service.impl;

import com.petcare.backend.dto.common.PageResponse;
import com.petcare.backend.dto.pet.request.CreateCoParentInvitationRequest;
import com.petcare.backend.dto.pet.request.CreatePetRequest;
import com.petcare.backend.dto.pet.request.UpdatePetRequest;
import com.petcare.backend.dto.pet.request.UpdateCoParentRoleRequest;
import com.petcare.backend.dto.pet.response.CoParentResponse;
import com.petcare.backend.dto.pet.response.CoParentInvitationResponse;
import com.petcare.backend.dto.pet.response.PetResponse;
import com.petcare.backend.dto.pet.response.PetSummaryResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.ConflictException;
import com.petcare.backend.exception.ForbiddenException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.Breed;
import com.petcare.backend.model.CoParentInvitation;
import com.petcare.backend.model.HealthCondition;
import com.petcare.backend.model.Pet;
import com.petcare.backend.model.PetCoParent;
import com.petcare.backend.model.PetTimelineEvent;
import com.petcare.backend.model.PetVaccination;
import com.petcare.backend.model.Species;
import com.petcare.backend.model.User;
import com.petcare.backend.model.WeightLog;
import com.petcare.backend.model.VaccineTemplate;
import com.petcare.backend.repository.BreedRepository;
import com.petcare.backend.repository.CoParentInvitationRepository;
import com.petcare.backend.repository.HealthConditionRepository;
import com.petcare.backend.repository.PetCoParentRepository;
import com.petcare.backend.repository.PetRepository;
import com.petcare.backend.repository.PetTimelineEventRepository;
import com.petcare.backend.repository.SpeciesRepository;
import com.petcare.backend.repository.UserRepository;
import com.petcare.backend.repository.WeightLogRepository;
import com.petcare.backend.repository.PetVaccinationRepository;
import com.petcare.backend.repository.VaccineTemplateRepository;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.FileStorageService;
import com.petcare.backend.service.NotificationService;
import com.petcare.backend.service.PetService;
import com.petcare.backend.util.BreedCategoryHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PetServiceImpl implements PetService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final SpeciesRepository speciesRepository;
    private final BreedRepository breedRepository;
    private final PetCoParentRepository coParentRepository;
    private final CoParentInvitationRepository invitationRepository;
    private final WeightLogRepository weightLogRepository;
    private final HealthConditionRepository healthConditionRepository;
    private final PetTimelineEventRepository petTimelineEventRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;
    private final PetVaccinationRepository petVaccinationRepository;
    private final VaccineTemplateRepository vaccineTemplateRepository;

    // ========================
    // PET CRUD
    // ========================

    @Override
    @Transactional
    public PetResponse createPet(UserPrincipal principal, CreatePetRequest request) {
        return createPet(principal.getId(), request, null);
    }

    @Override
    @Transactional
    public PetResponse createPet(Long currentUserId, CreatePetRequest request, MultipartFile avatar) {
        User owner = getActiveUser(currentUserId);
        String uploadedAvatarUrl = null;
        try {
            if (avatar != null && !avatar.isEmpty()) {
                uploadedAvatarUrl = fileStorageService.storePetAvatar(avatar, currentUserId).getMediaUrl();
                registerNewAvatarRollback(uploadedAvatarUrl);
            }

            Pet pet = new Pet();
            pet.setOwner(owner);
            pet.setVaccinePlanStatus(Pet.VaccinePlanStatus.NOT_CONFIGURED);
            pet.setName(request.getName().trim());
            if (uploadedAvatarUrl != null) pet.setAvatarUrl(uploadedAvatarUrl);
            else if (StringUtils.hasText(request.getAvatarUrl())) pet.setAvatarUrl(request.getAvatarUrl().trim());

            applyPetFields(pet, request.getSpeciesId(), request.getCustomSpeciesName(),
                    request.getBreedId(), request.getCustomBreedName(),
                    request.getGender(), request.getDateOfBirth(), request.getEstimatedAgeMonths(),
                    request.getCurrentWeight(), request.getColorFeatures(), request.getSpayedStatus(),
                    request.getNotes(), request.getStatus());

            Pet saved = petRepository.save(pet);
            initializePetRecords(saved, owner, request);
            return toPetResponse(saved, "owner");
        } catch (RuntimeException ex) {
            if (uploadedAvatarUrl != null) {
                try { fileStorageService.deleteByUrl(uploadedAvatarUrl); }
                catch (RuntimeException cleanupEx) { log.warn("Could not clean up pet avatar", cleanupEx); }
            }
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PetSummaryResponse> getMyPets(UserPrincipal principal) {
        Long userId = principal.getId();
        return petRepository.findAllAccessibleByUserId(userId).stream()
                .map(pet -> {
                    String role = resolveRole(pet, userId);
                    return PetSummaryResponse.from(pet, role);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PetResponse getPetById(UserPrincipal principal, Long petId) {
        Long userId = principal.getId();
        Pet pet = petRepository.findByIdAndAccessibleByUserId(petId, userId)
                .orElseThrow(() -> new BadRequestException("Thú cưng không tồn tại hoặc bạn không có quyền truy cập"));
        return toPetResponse(pet, resolveRole(pet, userId));
    }

    @Override
    @Transactional
    public PetResponse updatePet(UserPrincipal principal, Long petId, UpdatePetRequest request) {
        return updatePet(principal.getId(), petId, request, null);
    }

    @Override
    @Transactional
    public PetResponse updatePet(Long currentUserId, Long petId, UpdatePetRequest request, MultipartFile avatar) {
        Pet pet = petRepository.findByIdAndAccessibleByUserId(petId, currentUserId)
                .orElseThrow(() -> new BadRequestException("Thú cưng không tồn tại hoặc bạn không có quyền truy cập"));

        String role = resolveRole(pet, currentUserId);
        if ("viewer".equals(role)) {
            throw new ForbiddenException("Bạn không có quyền chỉnh sửa thú cưng này");
        }

        if (avatar != null && !avatar.isEmpty()) {
            String oldAvatarUrl = pet.getAvatarUrl();
            String newAvatarUrl = fileStorageService.storePetAvatar(avatar, currentUserId).getMediaUrl();
            pet.setAvatarUrl(newAvatarUrl);
            registerAvatarCleanup(oldAvatarUrl, newAvatarUrl);
        }

        if (StringUtils.hasText(request.getName())) {
            pet.setName(request.getName().trim());
        }

        applyPetFields(pet, request.getSpeciesId(), request.getCustomSpeciesName(), request.getBreedId(),
                request.getCustomBreedName(),
                request.getGender(), request.getDateOfBirth(),
                request.getEstimatedAgeMonths(), request.getCurrentWeight(),
                request.getColorFeatures(), request.getSpayedStatus(),
                request.getNotes(), request.getStatus());

        replaceHealthConditions(pet, request.getAllergies(), HealthCondition.ConditionType.allergy);
        replaceHealthConditions(pet, request.getMedicalConditions(), HealthCondition.ConditionType.chronic_disease);

        Pet saved = petRepository.save(pet);
        return toPetResponse(saved, role);
    }

    @Override
    @Transactional
    public PetResponse archivePet(UserPrincipal principal, Long petId) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thú cưng"));

        if (!pet.getOwner().getId().equals(principal.getId())) {
            throw new ForbiddenException("Chỉ chủ nuôi mới có thể lưu trữ thú cưng");
        }
        if (pet.getStatus() == Pet.PetStatus.deceased) {
            throw new ConflictException("Không thể lưu trữ thú cưng đã mất");
        }
        if (pet.getStatus() == Pet.PetStatus.archived) {
            return toPetResponse(pet, "owner");
        }

        pet.setStatus(Pet.PetStatus.archived);
        return toPetResponse(petRepository.save(pet), "owner");
    }

    @Override
    @Transactional
    public void deletePet(UserPrincipal principal, Long petId) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new BadRequestException("Thú cưng không tồn tại"));

        if (!pet.getOwner().getId().equals(principal.getId())) {
            throw new BadRequestException("Chỉ chủ nhân mới có thể xóa thú cưng");
        }

        petRepository.delete(pet);
    }

    // ========================
    // CO-PARENT
    // ========================

    @Override
    @Transactional(readOnly = true)
    public List<CoParentResponse> getCoParents(UserPrincipal principal, Long petId) {
        Long userId = principal.getId();
        petRepository.findByIdAndAccessibleByUserId(petId, userId)
                .orElseThrow(() -> new BadRequestException("Thú cưng không tồn tại hoặc bạn không có quyền truy cập"));

        return coParentRepository.findByPetId(petId).stream()
                .map(CoParentResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CoParentResponse updateCoParentRole(Long currentUserId, Long petId, Long coParentId,
                                               UpdateCoParentRoleRequest request) {
        Pet pet = getPetAndEnsureOwner(currentUserId, petId,
                "Only pet owner can update co-parent role.");
        PetCoParent coParent = coParentRepository.findByIdAndPetId(coParentId, petId)
                .orElseThrow(() -> new ResourceNotFoundException("Co-parent not found."));

        PetCoParent.CoParentRole newRole;
        try {
            newRole = PetCoParent.CoParentRole.valueOf(request.getRole().trim().toLowerCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid co-parent role. Allowed values: editor, viewer.");
        }

        if (coParent.getRole() == newRole) {
            return CoParentResponse.from(coParent);
        }

        coParent.setRole(newRole);
        PetCoParent saved = coParentRepository.save(coParent);
        if (!saved.getUser().getId().equals(currentUserId)) {
            notificationService.createNotification(saved.getUser().getId(), currentUserId,
                    "Quyền đồng chăm sóc đã được cập nhật",
                    pet.getOwner().getFullName() + " đã cập nhật quyền đồng chăm sóc của bạn với bé "
                            + pet.getName() + " thành " + newRole.name() + ".",
                    "co_parent_role_updated", pet.getId());
        }
        return CoParentResponse.from(saved);
    }

    @Override
    @Transactional
    public void removeCoParent(Long currentUserId, Long petId, Long coParentId) {
        Pet pet = getPetAndEnsureOwner(currentUserId, petId,
                "Only pet owner can remove co-parent.");
        PetCoParent coParent = coParentRepository.findByIdAndPetId(coParentId, petId)
                .orElseThrow(() -> new ResourceNotFoundException("Co-parent not found."));
        User removedUser = coParent.getUser();
        coParentRepository.delete(coParent);
        if (!removedUser.getId().equals(currentUserId)) {
            notificationService.createNotification(removedUser.getId(), currentUserId,
                    "Bạn đã bị gỡ khỏi đồng chăm sóc",
                    pet.getOwner().getFullName() + " đã gỡ bạn khỏi danh sách đồng chăm sóc bé "
                            + pet.getName() + ".",
                    "co_parent_removed", pet.getId());
        }
    }

    @Override @Transactional
    public CoParentInvitationResponse createCoParentInvitation(
            Long currentUserId, Long petId, CreateCoParentInvitationRequest request) {
        User inviter = getActiveUser(currentUserId);
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));
        if (pet.getStatus() != Pet.PetStatus.active) throw new ConflictException("Pet is not active");
        if (!pet.getOwner().getId().equals(currentUserId))
            throw new ForbiddenException("Only pet owner can send invitations");
        String email = request.getInviteeEmail().trim().toLowerCase();
        User invitee = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Invitee email does not exist"));
        if (!"active".equalsIgnoreCase(invitee.getStatus())) throw new ConflictException("Invitee is not active");
        if (invitee.getId().equals(currentUserId)) throw new BadRequestException("You cannot invite yourself");
        PetCoParent.CoParentRole role;
        try { role = PetCoParent.CoParentRole.valueOf(request.getRole().trim().toLowerCase()); }
        catch (IllegalArgumentException ex) { throw new BadRequestException("Role must be editor or viewer"); }
        if (coParentRepository.existsByPetIdAndUserId(petId, invitee.getId()))
            throw new ConflictException("User is already a co-parent");
        if (invitationRepository.existsByPetIdAndInviteeUserIdAndStatus(
                petId, invitee.getId(), CoParentInvitation.InvitationStatus.pending))
            throw new ConflictException("Pending invitation already exists");
        CoParentInvitation item = new CoParentInvitation();
        item.setPet(pet); item.setInviter(inviter); item.setInviteeUser(invitee);
        item.setInviteeEmail(email); item.setRole(role); item.setInviteCode(generateUniqueInviteCode());
        item.setExpiresAt(LocalDateTime.now().plusHours(24));
        item = invitationRepository.save(item);
        notificationService.createNotification(invitee.getId(), inviter.getId(), "Lời mời đồng chăm sóc",
                inviter.getFullName() + " đã mời bạn đồng chăm sóc bé " + pet.getName()
                        + " với quyền " + role.name() + ".", "co_parent_invite", item.getId());
        return CoParentInvitationResponse.from(item);
    }

    @Override @Transactional(readOnly = true)
    public PageResponse<CoParentInvitationResponse> getIncomingInvitations(Long userId, int page, int size) {
        getActiveUser(userId);
        Page<CoParentInvitationResponse> result = invitationRepository
                .findByInviteeUserIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                        userId, CoParentInvitation.InvitationStatus.pending, LocalDateTime.now(), page(page, size))
                .map(CoParentInvitationResponse::from);
        return PageResponse.from(result);
    }

    @Override @Transactional(readOnly = true)
    public PageResponse<CoParentInvitationResponse> getOutgoingInvitations(Long userId, int page, int size) {
        getActiveUser(userId);
        return PageResponse.from(invitationRepository.findByInviterIdOrderByCreatedAtDesc(userId, page(page, size))
                .map(CoParentInvitationResponse::from));
    }

    @Override @Transactional(readOnly = true)
    public CoParentInvitationResponse getInvitation(Long userId, Long id) {
        getActiveUser(userId); CoParentInvitation item = invitation(id);
        if (!item.getInviter().getId().equals(userId)
                && (item.getInviteeUser() == null || !item.getInviteeUser().getId().equals(userId)))
            throw new ForbiddenException("You cannot view this invitation");
        return CoParentInvitationResponse.from(item);
    }

    @Override @Transactional
    public CoParentInvitationResponse acceptCoParentInvitation(Long userId, Long id) {
        User invitee = getActiveUser(userId); CoParentInvitation item = invitationForInvitee(id, userId);
        pending(item); Pet pet = item.getPet();
        if (pet.getStatus() != Pet.PetStatus.active) throw new ConflictException("Pet is not active");
        if (coParentRepository.existsByPetIdAndUserId(pet.getId(), userId))
            throw new ConflictException("User is already a co-parent");
        PetCoParent cp = new PetCoParent(); cp.setPet(pet); cp.setUser(invitee);
        cp.setRole(item.getRole()); cp.setInvitedBy(item.getInviter()); coParentRepository.save(cp);
        item.setStatus(CoParentInvitation.InvitationStatus.accepted); item.setAcceptedAt(LocalDateTime.now());
        notificationService.createNotification(item.getInviter().getId(), invitee.getId(),
                "Lời mời đã được chấp nhận", invitee.getFullName()
                        + " đã chấp nhận lời mời đồng chăm sóc bé " + pet.getName() + ".",
                "co_parent_accepted", pet.getId());
        return CoParentInvitationResponse.from(invitationRepository.save(item));
    }

    @Override @Transactional
    public CoParentInvitationResponse declineCoParentInvitation(Long userId, Long id) {
        User invitee = getActiveUser(userId); CoParentInvitation item = invitationForInvitee(id, userId);
        pending(item); item.setStatus(CoParentInvitation.InvitationStatus.declined);
        item.setDeclinedAt(LocalDateTime.now());
        notificationService.createNotification(item.getInviter().getId(), invitee.getId(), "Lời mời bị từ chối",
                invitee.getFullName() + " đã từ chối lời mời đồng chăm sóc bé " + item.getPet().getName() + ".",
                "co_parent_declined", item.getPet().getId());
        return CoParentInvitationResponse.from(invitationRepository.save(item));
    }

    @Override @Transactional
    public CoParentInvitationResponse revokeCoParentInvitation(Long userId, Long id) {
        getActiveUser(userId); CoParentInvitation item = invitation(id);
        if (!item.getInviter().getId().equals(userId) && !item.getPet().getOwner().getId().equals(userId))
            throw new ForbiddenException("You cannot revoke this invitation");
        if (item.getStatus() != CoParentInvitation.InvitationStatus.pending)
            throw new ConflictException("Invitation is no longer pending");
        item.setStatus(CoParentInvitation.InvitationStatus.revoked); item.setRevokedAt(LocalDateTime.now());
        if (item.getInviteeUser() != null) notificationService.createNotification(
                item.getInviteeUser().getId(), userId, "Lời mời đã được thu hồi",
                "Lời mời đồng chăm sóc bé " + item.getPet().getName() + " đã được thu hồi.",
                "co_parent_revoked", item.getId());
        return CoParentInvitationResponse.from(invitationRepository.save(item));
    }

    // ========================
    // PRIVATE HELPERS
    // ========================

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Người dùng không tồn tại"));
    }

    private User getActiveUser(Long userId) {
        User user = getUser(userId);
        if (!"active".equalsIgnoreCase(user.getStatus())) throw new BadRequestException("Current user is not active");
        return user;
    }

    private Pet getPetAndEnsureOwner(Long currentUserId, Long petId, String forbiddenMessage) {
        getActiveUser(currentUserId);
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found."));
        if (pet.getStatus() != Pet.PetStatus.active) {
            throw new ConflictException("Pet is not active.");
        }
        if (!pet.getOwner().getId().equals(currentUserId)) {
            throw new ForbiddenException(forbiddenMessage);
        }
        return pet;
    }

    private PetResponse toPetResponse(Pet pet, String role) {
        PetResponse response = PetResponse.from(pet, role);
        List<HealthCondition> conditions = healthConditionRepository.findByPetIdAndIsActiveTrue(pet.getId());
        List<String> allergies = new ArrayList<>();
        List<String> medicalConditions = new ArrayList<>();
        for (HealthCondition condition : conditions) {
            if (condition.getType() == HealthCondition.ConditionType.allergy) {
                allergies.add(condition.getTitle());
            } else if (condition.getType() == HealthCondition.ConditionType.chronic_disease) {
                medicalConditions.add(condition.getTitle());
            }
        }
        response.setAllergies(allergies);
        response.setMedicalConditions(medicalConditions);
        return response;
    }

    private String resolveRole(Pet pet, Long userId) {
        if (pet.getOwner().getId().equals(userId)) {
            return "owner";
        }
        return coParentRepository.findByPetIdAndUserId(pet.getId(), userId)
                .map(cp -> cp.getRole().name())
                .orElse("viewer");
    }

    private void applyPetFields(Pet pet, Long speciesId, String customSpeciesName, Long breedId, String customBreedName,
                                Pet.Gender gender, java.time.LocalDate dateOfBirth,
                                Integer estimatedAgeMonths, java.math.BigDecimal currentWeight,
                                String colorFeatures, Pet.SpayedStatus spayedStatus,
                                String notes, Pet.PetStatus status) {

        if (speciesId != null && speciesId > 0) {
            Species species = speciesRepository.findById(speciesId)
                    .orElseThrow(() -> new BadRequestException("Loài không tồn tại"));
            pet.setSpecies(species);
        } else if (StringUtils.hasText(customSpeciesName)) {
            pet.setSpecies(resolveCustomSpecies(customSpeciesName));
        }

        if (breedId != null) {
            Breed breed = breedRepository.findById(breedId)
                    .orElseThrow(() -> new BadRequestException("Giống không tồn tại"));
            if (pet.getSpecies() != null && !breed.getSpecies().getId().equals(pet.getSpecies().getId())) {
                throw new BadRequestException("Giống không thuộc loài đã chọn");
            }
            pet.setBreed(breed);
            applyCustomBreedName(pet, breed, customBreedName);
        } else if (StringUtils.hasText(customBreedName)) {
            pet.setBreed(null);
            pet.setCustomBreedName(customBreedName.trim());
        }

        if (gender != null) pet.setGender(gender);
        if (dateOfBirth != null) pet.setDateOfBirth(dateOfBirth);
        if (estimatedAgeMonths != null) pet.setEstimatedAgeMonths(estimatedAgeMonths);
        if (currentWeight != null) pet.setCurrentWeight(currentWeight);
        if (StringUtils.hasText(colorFeatures)) pet.setColorFeatures(colorFeatures);
        if (spayedStatus != null) pet.setSpayedStatus(spayedStatus);
        if (StringUtils.hasText(notes)) pet.setNotes(notes);
        if (status != null) pet.setStatus(status);
    }

    private Species resolveCustomSpecies(String customSpeciesName) {
        String normalizedName = customSpeciesName.trim();
        return speciesRepository.findByNameIgnoreCase(normalizedName)
                .orElseGet(() -> {
                    Species species = new Species();
                    species.setName(normalizedName);
                    species.setActive(true);
                    return speciesRepository.save(species);
                });
    }

    private void initializePetRecords(Pet pet, User owner, CreatePetRequest request) {
        saveHealthConditions(pet, request.getAllergies(), HealthCondition.ConditionType.allergy);
        saveHealthConditions(pet, request.getMedicalConditions(), HealthCondition.ConditionType.chronic_disease);

        if (pet.getCurrentWeight() != null) {
            WeightLog weightLog = new WeightLog();
            weightLog.setPet(pet);
            weightLog.setWeight(pet.getCurrentWeight());
            weightLog.setLoggedDate(LocalDate.now());
            weightLog.setLoggedBy(owner);
            weightLogRepository.save(weightLog);
        }

        if (pet.getSpecies() != null) generateVaccinationSchedule(pet);

        PetTimelineEvent event = new PetTimelineEvent();
        event.setPet(pet);
        event.setEventType(PetTimelineEvent.EventType.profile_created);
        event.setReferenceId(pet.getId());
        event.setEventDate(LocalDate.now());
        event.setSummary("Hồ sơ bé " + pet.getName() + " đã được khởi tạo thành công.");
        petTimelineEventRepository.save(event);
    }

    private void saveHealthConditions(Pet pet, List<String> titles, HealthCondition.ConditionType type) {
        if (titles == null) {
            return;
        }
        for (String title : titles) {
            if (!StringUtils.hasText(title)) {
                continue;
            }
            HealthCondition condition = new HealthCondition();
            condition.setPet(pet);
            condition.setType(type);
            condition.setTitle(title.trim());
            condition.setIsActive(true);
            healthConditionRepository.save(condition);
        }
    }

    private void applyCustomBreedName(Pet pet, Breed breed, String customBreedName) {
        if (BreedCategoryHelper.isOtherBreed(breed)) {
            if (!StringUtils.hasText(customBreedName)) {
                throw new BadRequestException("Vui lòng nhập giống thú cưng khi chọn \"Khác\"");
            }
            pet.setCustomBreedName(customBreedName.trim());
        } else {
            pet.setCustomBreedName(null);
        }
    }

    private void replaceHealthConditions(Pet pet, List<String> titles, HealthCondition.ConditionType type) {
        if (titles == null) {
            return;
        }

        List<HealthCondition> activeConditions = healthConditionRepository
                .findByPetIdAndTypeAndIsActiveTrue(pet.getId(), type);
        activeConditions.forEach(condition -> condition.setIsActive(false));
        healthConditionRepository.saveAll(activeConditions);

        titles.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .forEach(title -> {
                    HealthCondition condition = new HealthCondition();
                    condition.setPet(pet);
                    condition.setType(type);
                    condition.setTitle(title);
                    condition.setIsActive(true);
                    healthConditionRepository.save(condition);
                });
    }

    private void generateVaccinationSchedule(Pet pet) {
        LocalDate birth = pet.getDateOfBirth() != null ? pet.getDateOfBirth()
                : pet.getEstimatedAgeMonths() != null && pet.getEstimatedAgeMonths() > 0
                ? LocalDate.now().minusMonths(pet.getEstimatedAgeMonths()) : LocalDate.now();
        for (VaccineTemplate template : vaccineTemplateRepository.findBySpeciesId(pet.getSpecies().getId())) {
            if (!Boolean.TRUE.equals(template.getActive())) continue;
            com.petcare.backend.model.PetVaccination vaccination = new com.petcare.backend.model.PetVaccination();
            vaccination.setPet(pet); vaccination.setVaccineTemplate(template);
            vaccination.setVaccineName(template.getVaccineName()); vaccination.setDoseNumber(template.getDoseNumber());
            vaccination.setStatus(com.petcare.backend.model.PetVaccination.VaccinationStatus.scheduled);
            vaccination.setScheduledDate(birth.plusWeeks(template.getRecommendedAgeWeeks()));
            petVaccinationRepository.save(vaccination);
        }
    }

    private String generateInviteCode() {
        return "PET-" + String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    private String generateUniqueInviteCode() {
        for (int i = 0; i < 5; i++) { String code = generateInviteCode();
            if (!invitationRepository.existsByInviteCode(code)) return code; }
        throw new ConflictException("Could not generate a unique invitation code");
    }

    private PageRequest page(int page, int size) {
        if (page < 0 || size <= 0) throw new BadRequestException("Invalid page or size");
        return PageRequest.of(page, Math.min(size, 50));
    }

    private CoParentInvitation invitation(Long id) {
        return invitationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));
    }

    private CoParentInvitation invitationForInvitee(Long id, Long userId) {
        CoParentInvitation item = invitation(id);
        if (item.getInviteeUser() == null || !item.getInviteeUser().getId().equals(userId))
            throw new ForbiddenException("This invitation does not belong to you");
        return item;
    }

    private void pending(CoParentInvitation item) {
        if (item.getStatus() != CoParentInvitation.InvitationStatus.pending)
            throw new ConflictException("Invitation is no longer pending");
        if (!item.getExpiresAt().isAfter(LocalDateTime.now()))
            throw new ConflictException("Invitation has expired");
    }

    private void registerAvatarCleanup(String oldUrl, String newUrl) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                try {
                    fileStorageService.deleteByUrl(status == STATUS_COMMITTED ? oldUrl : newUrl);
                } catch (RuntimeException ex) { log.warn("Could not clean up pet avatar", ex); }
            }
        });
    }

    private void registerNewAvatarRollback(String newUrl) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    try { fileStorageService.deleteByUrl(newUrl); }
                    catch (RuntimeException ex) { log.warn("Could not clean up new pet avatar", ex); }
                }
            }
        });
    }
}

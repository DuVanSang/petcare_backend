package com.petcare.backend.service.impl;

import com.petcare.backend.dto.pet.request.AcceptInvitationRequest;
import com.petcare.backend.dto.pet.request.CreatePetRequest;
import com.petcare.backend.dto.pet.request.InviteCoParentRequest;
import com.petcare.backend.dto.pet.request.UpdatePetRequest;
import com.petcare.backend.dto.pet.response.CoParentResponse;
import com.petcare.backend.dto.pet.response.PetResponse;
import com.petcare.backend.dto.pet.response.PetSummaryResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.model.Breed;
import com.petcare.backend.model.CoParentInvitation;
import com.petcare.backend.model.Pet;
import com.petcare.backend.model.PetCoParent;
import com.petcare.backend.model.Species;
import com.petcare.backend.model.User;
import com.petcare.backend.repository.BreedRepository;
import com.petcare.backend.repository.CoParentInvitationRepository;
import com.petcare.backend.repository.PetCoParentRepository;
import com.petcare.backend.repository.PetRepository;
import com.petcare.backend.repository.SpeciesRepository;
import com.petcare.backend.repository.UserRepository;
import com.petcare.backend.security.UserPrincipal;
import com.petcare.backend.service.PetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
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

    // ========================
    // PET CRUD
    // ========================

    @Override
    @Transactional
    public PetResponse createPet(UserPrincipal principal, CreatePetRequest request) {
        User owner = getUser(principal.getId());

        Pet pet = new Pet();
        pet.setOwner(owner);
        pet.setName(request.getName().trim());

        applyPetFields(pet, request.getSpeciesId(), request.getBreedId(),
                request.getGender(), request.getDateOfBirth(),
                request.getEstimatedAgeMonths(), request.getCurrentWeight(),
                request.getColorFeatures(), request.getSpayedStatus(),
                request.getMicrochipNumber(), request.getNotes(), null);

        Pet saved = petRepository.save(pet);
        return PetResponse.from(saved, "owner");
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
        return PetResponse.from(pet, resolveRole(pet, userId));
    }

    @Override
    @Transactional
    public PetResponse updatePet(UserPrincipal principal, Long petId, UpdatePetRequest request) {
        Long userId = principal.getId();
        Pet pet = petRepository.findByIdAndAccessibleByUserId(petId, userId)
                .orElseThrow(() -> new BadRequestException("Thú cưng không tồn tại hoặc bạn không có quyền truy cập"));

        String role = resolveRole(pet, userId);
        if ("viewer".equals(role)) {
            throw new BadRequestException("Bạn không có quyền chỉnh sửa thú cưng này");
        }

        if (StringUtils.hasText(request.getName())) {
            pet.setName(request.getName().trim());
        }

        applyPetFields(pet, request.getSpeciesId(), request.getBreedId(),
                request.getGender(), request.getDateOfBirth(),
                request.getEstimatedAgeMonths(), request.getCurrentWeight(),
                request.getColorFeatures(), request.getSpayedStatus(),
                request.getMicrochipNumber(), request.getNotes(), request.getStatus());

        Pet saved = petRepository.save(pet);
        return PetResponse.from(saved, role);
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
    public void inviteCoParent(UserPrincipal principal, Long petId, InviteCoParentRequest request) {
        Long userId = principal.getId();
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new BadRequestException("Thú cưng không tồn tại"));

        // Chỉ owner mới được mời
        if (!pet.getOwner().getId().equals(userId)) {
            throw new BadRequestException("Chỉ chủ nhân mới có thể mời đồng nuôi");
        }

        String inviteeEmail = request.getInviteeEmail().trim().toLowerCase();

        // Không tự mời chính mình
        User owner = getUser(userId);
        if (owner.getEmail().equals(inviteeEmail)) {
            throw new BadRequestException("Không thể mời chính mình");
        }

        // Kiểm tra đã là co-parent chưa
        userRepository.findByEmail(inviteeEmail).ifPresent(invitee -> {
            if (coParentRepository.existsByPetIdAndUserId(petId, invitee.getId())) {
                throw new BadRequestException("Người dùng này đã là đồng nuôi của thú cưng");
            }
        });

        // Huỷ invitation pending cũ nếu có
        invitationRepository.findByPetIdAndInviteeEmailAndStatus(
                petId, inviteeEmail, CoParentInvitation.InvitationStatus.pending
        ).ifPresent(old -> {
            old.setStatus(CoParentInvitation.InvitationStatus.cancelled);
            invitationRepository.save(old);
        });

        // Tạo invitation mới
        CoParentInvitation invitation = new CoParentInvitation();
        invitation.setPet(pet);
        invitation.setInviter(owner);
        invitation.setInviteeEmail(inviteeEmail);
        invitation.setInviteCode(generateInviteCode());
        invitation.setRole(request.getRole());
        invitation.setExpiresAt(LocalDateTime.now().plusDays(7));
        invitationRepository.save(invitation);

        log.info("Co-parent invite code for {} to join pet '{}': {}",
                inviteeEmail, pet.getName(), invitation.getInviteCode());
    }

    @Override
    @Transactional
    public void acceptInvitation(UserPrincipal principal, AcceptInvitationRequest request) {
        CoParentInvitation invitation = invitationRepository.findByInviteCode(request.getInviteCode())
                .orElseThrow(() -> new BadRequestException("Mã mời không hợp lệ"));

        if (invitation.getStatus() != CoParentInvitation.InvitationStatus.pending) {
            throw new BadRequestException("Lời mời này không còn hiệu lực");
        }

        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus(CoParentInvitation.InvitationStatus.expired);
            invitationRepository.save(invitation);
            throw new BadRequestException("Lời mời đã hết hạn");
        }

        User acceptor = getUser(principal.getId());

        // Email phải khớp với email được mời
        if (!acceptor.getEmail().equalsIgnoreCase(invitation.getInviteeEmail())) {
            throw new BadRequestException("Lời mời này không dành cho tài khoản của bạn");
        }

        // Tránh thêm trùng
        if (coParentRepository.existsByPetIdAndUserId(invitation.getPet().getId(), acceptor.getId())) {
            throw new BadRequestException("Bạn đã là đồng nuôi của thú cưng này");
        }

        PetCoParent coParent = new PetCoParent();
        coParent.setPet(invitation.getPet());
        coParent.setUser(acceptor);
        coParent.setRole(invitation.getRole());
        coParent.setInvitedBy(invitation.getInviter());
        coParentRepository.save(coParent);

        invitation.setStatus(CoParentInvitation.InvitationStatus.accepted);
        invitationRepository.save(invitation);
    }

    // ========================
    // PRIVATE HELPERS
    // ========================

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Người dùng không tồn tại"));
    }

    private String resolveRole(Pet pet, Long userId) {
        if (pet.getOwner().getId().equals(userId)) {
            return "owner";
        }
        return coParentRepository.findByPetIdAndUserId(pet.getId(), userId)
                .map(cp -> cp.getRole().name())
                .orElse("viewer");
    }

    private void applyPetFields(Pet pet, Long speciesId, Long breedId,
                                Pet.Gender gender, java.time.LocalDate dateOfBirth,
                                Integer estimatedAgeMonths, java.math.BigDecimal currentWeight,
                                String colorFeatures, Pet.SpayedStatus spayedStatus,
                                String microchipNumber, String notes, Pet.PetStatus status) {

        if (speciesId != null) {
            Species species = speciesRepository.findById(speciesId)
                    .orElseThrow(() -> new BadRequestException("Loài không tồn tại"));
            pet.setSpecies(species);
        }

        if (breedId != null) {
            Breed breed = breedRepository.findById(breedId)
                    .orElseThrow(() -> new BadRequestException("Giống không tồn tại"));
            // Validate breed thuộc species
            if (pet.getSpecies() != null && !breed.getSpecies().getId().equals(pet.getSpecies().getId())) {
                throw new BadRequestException("Giống không thuộc loài đã chọn");
            }
            pet.setBreed(breed);
        }

        if (gender != null) pet.setGender(gender);
        if (dateOfBirth != null) pet.setDateOfBirth(dateOfBirth);
        if (estimatedAgeMonths != null) pet.setEstimatedAgeMonths(estimatedAgeMonths);
        if (currentWeight != null) pet.setCurrentWeight(currentWeight);
        if (StringUtils.hasText(colorFeatures)) pet.setColorFeatures(colorFeatures);
        if (spayedStatus != null) pet.setSpayedStatus(spayedStatus);
        if (StringUtils.hasText(microchipNumber)) pet.setMicrochipNumber(microchipNumber);
        if (StringUtils.hasText(notes)) pet.setNotes(notes);
        if (status != null) pet.setStatus(status);
    }

    private String generateInviteCode() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

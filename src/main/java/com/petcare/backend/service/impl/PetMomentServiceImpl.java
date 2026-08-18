package com.petcare.backend.service.impl;

import com.petcare.backend.dto.locket.response.PetMomentReactionDto;
import com.petcare.backend.dto.locket.response.PetMomentResponse;
import com.petcare.backend.dto.upload.UploadFileResponse;
import com.petcare.backend.exception.BadRequestException;
import com.petcare.backend.exception.ResourceNotFoundException;
import com.petcare.backend.model.Pet;
import com.petcare.backend.model.PetMoment;
import com.petcare.backend.model.PetMomentReaction;
import com.petcare.backend.model.User;
import com.petcare.backend.repository.PetCoParentRepository;
import com.petcare.backend.repository.PetMomentReactionRepository;
import com.petcare.backend.repository.PetMomentRepository;
import com.petcare.backend.repository.PetRepository;
import com.petcare.backend.repository.UserRepository;
import com.petcare.backend.service.FileStorageService;
import com.petcare.backend.service.PetMomentService;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class PetMomentServiceImpl implements PetMomentService {

    private final PetMomentRepository momentRepository;
    private final PetMomentReactionRepository reactionRepository;
    private final PetRepository petRepository;
    private final PetCoParentRepository coParentRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public PetMomentResponse createMoment(
            Long currentUserId,
            Long petId,
            String caption,
            String locationName,
            String moodTag,
            String audience,
            MultipartFile file
    ) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Thú cưng không tồn tại"));

        // Check if user is owner or co-parent
        boolean isOwner = pet.getOwner().getId().equals(currentUserId);
        boolean isCoParent = coParentRepository.existsByPetIdAndUserId(petId, currentUserId);
        if (!isOwner && !isCoParent) {
            throw new BadRequestException("Bạn không có quyền đăng khoảnh khắc cho thú cưng này");
        }

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Vui lòng chụp một bức ảnh để gửi khoảnh khắc");
        }

        UploadFileResponse uploadRes = fileStorageService.storeMomentMediaFile(file);

        String validAudience = "CO_PARENTS".equalsIgnoreCase(audience) ? "CO_PARENTS" : "FRIENDS";

        PetMoment moment = PetMoment.builder()
                .pet(pet)
                .user(user)
                .mediaUrl(uploadRes.getMediaUrl())
                .caption(StringUtils.hasText(caption) ? caption.trim() : null)
                .locationName(StringUtils.hasText(locationName) ? locationName.trim() : null)
                .moodTag(StringUtils.hasText(moodTag) ? moodTag.trim() : "PLAYFUL")
                .audience(validAudience)
                .createdAt(Instant.now())
                .build();

        PetMoment saved = momentRepository.save(moment);
        log.info("📸 [LOCKET] Tạo khoảnh khắc mới ID={} cho Pet={} (Audience={})", saved.getId(), pet.getName(), validAudience);

        return mapToResponse(saved, currentUserId, List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PetMomentResponse> getFeedMoments(Long currentUserId) {
        Instant since = Instant.now().minus(Duration.ofHours(24));
        List<PetMoment> moments = momentRepository.findActiveFeedMoments(currentUserId, since);

        if (moments.isEmpty()) {
            return List.of();
        }

        List<Long> momentIds = moments.stream().map(PetMoment::getId).toList();
        List<PetMomentReaction> allReactions = reactionRepository.findByMomentIdIn(momentIds);

        Map<Long, List<PetMomentReaction>> reactionsByMoment = allReactions.stream()
                .collect(Collectors.groupingBy(r -> r.getMoment().getId()));

        return moments.stream()
                .map(m -> mapToResponse(m, currentUserId, reactionsByMoment.getOrDefault(m.getId(), List.of())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PetMomentResponse> getMyMomentsHistory(Long currentUserId) {
        List<PetMoment> moments = momentRepository.findByUserIdOrderByCreatedAtDesc(currentUserId);
        if (moments.isEmpty()) {
            return List.of();
        }

        List<Long> momentIds = moments.stream().map(PetMoment::getId).toList();
        List<PetMomentReaction> allReactions = reactionRepository.findByMomentIdIn(momentIds);

        Map<Long, List<PetMomentReaction>> reactionsByMoment = allReactions.stream()
                .collect(Collectors.groupingBy(r -> r.getMoment().getId()));

        return moments.stream()
                .map(m -> mapToResponse(m, currentUserId, reactionsByMoment.getOrDefault(m.getId(), List.of())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PetMomentResponse> getPetMomentsHistory(Long currentUserId, Long petId) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Thú cưng không tồn tại"));

        List<PetMoment> moments = momentRepository.findByPetIdOrderByCreatedAtDesc(petId);
        if (moments.isEmpty()) {
            return List.of();
        }

        List<Long> momentIds = moments.stream().map(PetMoment::getId).toList();
        List<PetMomentReaction> allReactions = reactionRepository.findByMomentIdIn(momentIds);

        Map<Long, List<PetMomentReaction>> reactionsByMoment = allReactions.stream()
                .collect(Collectors.groupingBy(r -> r.getMoment().getId()));

        return moments.stream()
                .map(m -> mapToResponse(m, currentUserId, reactionsByMoment.getOrDefault(m.getId(), List.of())))
                .toList();
    }

    @Override
    @Transactional
    public PetMomentReactionDto reactToMoment(Long currentUserId, Long momentId, String emoji) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        PetMoment moment = momentRepository.findById(momentId)
                .orElseThrow(() -> new ResourceNotFoundException("Khoảnh khắc không tồn tại"));

        if (!StringUtils.hasText(emoji)) {
            throw new BadRequestException("Emoji không hợp lệ");
        }

        PetMomentReaction reaction = PetMomentReaction.builder()
                .moment(moment)
                .user(user)
                .emoji(emoji.trim())
                .createdAt(Instant.now())
                .build();

        PetMomentReaction saved = reactionRepository.save(reaction);
        return mapReactionDto(saved);
    }

    @Override
    @Transactional
    public void deleteMoment(Long currentUserId, Long momentId) {
        PetMoment moment = momentRepository.findById(momentId)
                .orElseThrow(() -> new ResourceNotFoundException("Khoảnh khắc không tồn tại"));

        boolean isAuthor = moment.getUser().getId().equals(currentUserId);
        boolean isPetOwner = moment.getPet().getOwner().getId().equals(currentUserId);

        if (!isAuthor && !isPetOwner) {
            throw new BadRequestException("Bạn không có quyền xóa khoảnh khắc này");
        }

        reactionRepository.deleteAll(reactionRepository.findByMomentIdOrderByCreatedAtAsc(momentId));
        momentRepository.delete(moment);
        fileStorageService.deleteByUrl(moment.getMediaUrl());
        log.info("🗑️ [LOCKET] Đã xóa khoảnh khắc ID={}", momentId);
    }

    private PetMomentResponse mapToResponse(
            PetMoment m,
            Long currentUserId,
            List<PetMomentReaction> reactions
    ) {
        List<PetMomentReactionDto> reactionDtos = reactions.stream()
                .map(this::mapReactionDto)
                .toList();

        Map<String, Long> reactionCounts = reactions.stream()
                .collect(Collectors.groupingBy(PetMomentReaction::getEmoji, Collectors.counting()));

        return PetMomentResponse.builder()
                .id(m.getId())
                .petId(m.getPet().getId())
                .petName(m.getPet().getName())
                .petAvatarUrl(m.getPet().getAvatarUrl())
                .speciesName(m.getPet().getSpecies() != null ? m.getPet().getSpecies().getName() : null)
                .userId(m.getUser().getId())
                .userName(m.getUser().getFullName())
                .userAvatarUrl(m.getUser().getAvatarUrl())
                .mediaUrl(m.getMediaUrl())
                .caption(m.getCaption())
                .locationName(m.getLocationName())
                .moodTag(m.getMoodTag() != null ? m.getMoodTag() : "PLAYFUL")
                .audience(m.getAudience() != null ? m.getAudience() : "FRIENDS")
                .createdAt(m.getCreatedAt().toString())
                .reactions(reactionDtos)
                .reactionCounts(reactionCounts)
                .isMine(m.getUser().getId().equals(currentUserId))
                .build();
    }

    private PetMomentReactionDto mapReactionDto(PetMomentReaction r) {
        return PetMomentReactionDto.builder()
                .id(r.getId())
                .userId(r.getUser().getId())
                .userName(r.getUser().getFullName())
                .userAvatarUrl(r.getUser().getAvatarUrl())
                .emoji(r.getEmoji())
                .createdAt(r.getCreatedAt().toString())
                .build();
    }
}

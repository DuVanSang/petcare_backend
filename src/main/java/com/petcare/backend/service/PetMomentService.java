package com.petcare.backend.service;

import com.petcare.backend.dto.locket.response.PetMomentReactionDto;
import com.petcare.backend.dto.locket.response.PetMomentResponse;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface PetMomentService {

    PetMomentResponse createMoment(
            Long currentUserId,
            Long petId,
            String caption,
            String locationName,
            String moodTag,
            String audience,
            MultipartFile file
    );

    List<PetMomentResponse> getFeedMoments(Long currentUserId);

    List<PetMomentResponse> getPetMomentsHistory(Long currentUserId, Long petId);

    PetMomentReactionDto reactToMoment(Long currentUserId, Long momentId, String emoji);

    void deleteMoment(Long currentUserId, Long momentId);
}

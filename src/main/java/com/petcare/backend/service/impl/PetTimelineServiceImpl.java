package com.petcare.backend.service.impl;

import com.petcare.backend.model.Pet;
import com.petcare.backend.model.PetTimelineEvent;
import com.petcare.backend.model.Post;
import com.petcare.backend.repository.PetTimelineEventRepository;
import com.petcare.backend.service.PetTimelineService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PetTimelineServiceImpl implements PetTimelineService {
    private final PetTimelineEventRepository petTimelineEventRepository;

    @Override
    @Transactional
    public void createSocialPostEvent(Pet pet, Post post) {
        if (pet == null || post == null || pet.getId() == null || post.getId() == null) {
            return;
        }

        boolean exists = petTimelineEventRepository.existsByPetIdAndEventTypeAndReferenceId(
                pet.getId(),
                PetTimelineEvent.EventType.social_post,
                post.getId()
        );
        if (exists) {
            return;
        }

        PetTimelineEvent event = new PetTimelineEvent();
        event.setPet(pet);
        event.setEventType(PetTimelineEvent.EventType.social_post);
        event.setReferenceId(post.getId());
        event.setEventDate(LocalDate.now());
        event.setSummary("Chủ nuôi đã đăng một bài viết mới về bé " + pet.getName() + " lên mạng xã hội.");
        petTimelineEventRepository.save(event);
    }
}

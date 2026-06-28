package com.petcare.backend.repository;

import com.petcare.backend.model.PetTimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PetTimelineEventRepository extends JpaRepository<PetTimelineEvent, Long> {

    List<PetTimelineEvent> findByPetIdOrderByEventDateDescCreatedAtDesc(Long petId);

    boolean existsByPetIdAndEventTypeAndReferenceId(
            Long petId,
            PetTimelineEvent.EventType eventType,
            Long referenceId
    );
}

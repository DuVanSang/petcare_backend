package com.petcare.backend.repository;

import com.petcare.backend.model.PetTimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetTimelineEventRepository extends JpaRepository<PetTimelineEvent, Long> {
}

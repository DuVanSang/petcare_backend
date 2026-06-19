package com.petcare.backend.repository;

import com.petcare.backend.model.WeightLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WeightLogRepository extends JpaRepository<WeightLog, Long> {

    List<WeightLog> findByPetIdOrderByLoggedDateAsc(Long petId);
}

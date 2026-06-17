package com.petcare.backend.repository;

import com.petcare.backend.model.HealthCondition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthConditionRepository extends JpaRepository<HealthCondition, Long> {
}

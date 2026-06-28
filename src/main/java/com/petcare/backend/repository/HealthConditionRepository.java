package com.petcare.backend.repository;

import com.petcare.backend.model.HealthCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HealthConditionRepository extends JpaRepository<HealthCondition, Long> {
    List<HealthCondition> findByPetIdAndIsActiveTrue(Long petId);
    List<HealthCondition> findByPetIdAndTypeAndIsActiveTrue(
            Long petId, HealthCondition.ConditionType type);
}

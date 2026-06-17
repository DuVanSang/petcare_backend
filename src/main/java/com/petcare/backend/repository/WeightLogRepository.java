package com.petcare.backend.repository;

import com.petcare.backend.model.WeightLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeightLogRepository extends JpaRepository<WeightLog, Long> {
}

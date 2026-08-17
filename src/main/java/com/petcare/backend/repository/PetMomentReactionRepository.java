package com.petcare.backend.repository;

import com.petcare.backend.model.PetMomentReaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PetMomentReactionRepository extends JpaRepository<PetMomentReaction, Long> {

    List<PetMomentReaction> findByMomentIdOrderByCreatedAtAsc(Long momentId);

    @Query("SELECT r FROM PetMomentReaction r JOIN FETCH r.user WHERE r.moment.id IN :momentIds ORDER BY r.createdAt ASC")
    List<PetMomentReaction> findByMomentIdIn(@Param("momentIds") List<Long> momentIds);
}

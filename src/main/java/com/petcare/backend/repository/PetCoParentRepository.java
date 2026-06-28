package com.petcare.backend.repository;

import com.petcare.backend.model.PetCoParent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface PetCoParentRepository extends JpaRepository<PetCoParent, Long> {

    List<PetCoParent> findByPetId(Long petId);
    Optional<PetCoParent> findByIdAndPetId(Long id, Long petId);
    Page<PetCoParent> findByPetIdOrderByJoinedAtDesc(Long petId, Pageable pageable);

    Optional<PetCoParent> findByPetIdAndUserId(Long petId, Long userId);

    boolean existsByPetIdAndUserId(Long petId, Long userId);

    boolean existsByPetIdAndUserIdAndRole(Long petId, Long userId, PetCoParent.CoParentRole role);
}

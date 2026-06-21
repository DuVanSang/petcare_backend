package com.petcare.backend.repository;

import com.petcare.backend.model.PetCoParent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PetCoParentRepository extends JpaRepository<PetCoParent, Long> {

    List<PetCoParent> findByPetId(Long petId);

    Optional<PetCoParent> findByPetIdAndUserId(Long petId, Long userId);

    boolean existsByPetIdAndUserId(Long petId, Long userId);
}

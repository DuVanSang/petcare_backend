package com.petcare.backend.repository;

import com.petcare.backend.model.Pet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {

    List<Pet> findByOwnerId(Long ownerId);

    @Query("""
            SELECT COUNT(DISTINCT p.id)
            FROM Pet p
            WHERE p.owner.id = :userId
               OR EXISTS (
                   SELECT cp FROM PetCoParent cp
                   WHERE cp.pet.id = p.id AND cp.user.id = :userId
               )
            """)
    long countAccessiblePetsByUserId(@Param("userId") Long userId);

    long countByStatus(Pet.PetStatus status);

    @Query("""
            SELECT DISTINCT p FROM Pet p
            WHERE p.owner.id = :userId
               OR EXISTS (
                   SELECT cp FROM PetCoParent cp
                   WHERE cp.pet.id = p.id AND cp.user.id = :userId
               )
            ORDER BY p.createdAt DESC
            """)
    List<Pet> findAllAccessibleByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT p FROM Pet p
            WHERE p.id = :petId
              AND (
                  p.owner.id = :userId
                  OR EXISTS (
                      SELECT cp FROM PetCoParent cp
                      WHERE cp.pet.id = p.id AND cp.user.id = :userId
                  )
              )
            """)
    Optional<Pet> findByIdAndAccessibleByUserId(@Param("petId") Long petId, @Param("userId") Long userId);

    @Query("""
            SELECT p FROM Pet p
            JOIN p.owner owner
            LEFT JOIN p.species species
            WHERE (:keyword IS NULL
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(owner.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(owner.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:ownerId IS NULL OR owner.id = :ownerId)
              AND (:speciesId IS NULL OR species.id = :speciesId)
              AND (:status IS NULL OR p.status = :status)
              AND (:vaccinePlanStatus IS NULL OR p.vaccinePlanStatus = :vaccinePlanStatus)
            ORDER BY p.createdAt DESC
            """)
    Page<Pet> searchForAdmin(
            @Param("keyword") String keyword,
            @Param("ownerId") Long ownerId,
            @Param("speciesId") Long speciesId,
            @Param("status") Pet.PetStatus status,
            @Param("vaccinePlanStatus") Pet.VaccinePlanStatus vaccinePlanStatus,
            Pageable pageable
    );
}

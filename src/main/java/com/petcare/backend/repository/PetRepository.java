package com.petcare.backend.repository;

import com.petcare.backend.model.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {

    // Lấy pet mà user là owner
    List<Pet> findByOwnerId(Long ownerId);

    // Lấy tất cả pet mà user có thể truy cập (owner hoặc co-parent)
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

    // Lấy 1 pet cụ thể mà user có quyền truy cập
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
}

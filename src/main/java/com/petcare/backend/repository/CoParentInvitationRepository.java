package com.petcare.backend.repository;

import com.petcare.backend.model.CoParentInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface CoParentInvitationRepository extends JpaRepository<CoParentInvitation, Long> {

    boolean existsByInviteCode(String inviteCode);
    Page<CoParentInvitation> findByInviteeUserIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
            Long inviteeUserId, CoParentInvitation.InvitationStatus status,
            java.time.LocalDateTime now, Pageable pageable);
    Page<CoParentInvitation> findByInviterIdOrderByCreatedAtDesc(Long inviterId, Pageable pageable);
    boolean existsByPetIdAndInviteeUserIdAndStatus(
            Long petId, Long inviteeUserId, CoParentInvitation.InvitationStatus status);
}

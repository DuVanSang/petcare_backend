package com.petcare.backend.repository;

import com.petcare.backend.model.CoParentInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CoParentInvitationRepository extends JpaRepository<CoParentInvitation, Long> {

    Optional<CoParentInvitation> findByInviteCode(String inviteCode);

    // Tìm invitation đang pending cho pet và email cụ thể
    Optional<CoParentInvitation> findByPetIdAndInviteeEmailAndStatus(
            Long petId, String inviteeEmail, CoParentInvitation.InvitationStatus status);
}

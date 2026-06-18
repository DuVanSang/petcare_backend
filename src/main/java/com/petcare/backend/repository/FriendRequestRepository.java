package com.petcare.backend.repository;

import com.petcare.backend.model.FriendRequest;
import com.petcare.backend.model.enums.FriendRequestStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {
    Optional<FriendRequest> findBySender_IdAndReceiver_Id(Long senderId, Long receiverId);

    Optional<FriendRequest> findBySender_IdAndReceiver_IdAndStatus(
            Long senderId,
            Long receiverId,
            FriendRequestStatus status
    );

    Optional<FriendRequest> findByReceiver_IdAndSender_IdAndStatus(
            Long receiverId,
            Long senderId,
            FriendRequestStatus status
    );

    boolean existsBySender_IdAndReceiver_IdAndStatus(
            Long senderId,
            Long receiverId,
            FriendRequestStatus status
    );

    Page<FriendRequest> findByReceiver_IdAndStatusOrderByCreatedAtDesc(
            Long receiverId,
            FriendRequestStatus status,
            Pageable pageable
    );

    Page<FriendRequest> findBySender_IdAndStatusOrderByCreatedAtDesc(
            Long senderId,
            FriendRequestStatus status,
            Pageable pageable
    );

    @Query("""
            SELECT request
            FROM FriendRequest request
            WHERE (request.sender.id = :userId1 AND request.receiver.id = :userId2)
               OR (request.sender.id = :userId2 AND request.receiver.id = :userId1)
            ORDER BY request.updatedAt DESC
            """)
    List<FriendRequest> findBetweenOrderByUpdatedAtDesc(
            @Param("userId1") Long userId1,
            @Param("userId2") Long userId2
    );

    long countByReceiver_IdAndStatus(Long receiverId, FriendRequestStatus status);

    long countBySender_IdAndStatus(Long senderId, FriendRequestStatus status);
}

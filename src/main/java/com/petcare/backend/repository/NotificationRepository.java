package com.petcare.backend.repository;

import com.petcare.backend.model.Notification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByReceiver_IdOrderByCreatedAtDesc(Long receiverId);

    Page<Notification> findByReceiver_IdOrderByCreatedAtDesc(Long receiverId, Pageable pageable);

    long countByReceiver_IdAndIsReadFalse(Long receiverId);

    Optional<Notification> findByIdAndReceiver_Id(Long id, Long receiverId);

    boolean existsByReceiver_IdAndSender_IdAndTypeAndData(
            Long receiverId,
            Long senderId,
            String type,
            String data
    );
}

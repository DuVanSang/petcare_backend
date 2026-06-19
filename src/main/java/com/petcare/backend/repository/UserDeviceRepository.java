package com.petcare.backend.repository;

import com.petcare.backend.model.UserDevice;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {
    Optional<UserDevice> findByDeviceToken(String deviceToken);

    Optional<UserDevice> findByDeviceId(String deviceId);

    List<UserDevice> findByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<UserDevice> findByIdAndUserId(Long id, Long userId);
}

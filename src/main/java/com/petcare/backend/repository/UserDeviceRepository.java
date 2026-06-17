package com.petcare.backend.repository;

import com.petcare.backend.model.UserDevice;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {
    List<UserDevice> findByUserId(Long userId);

    Optional<UserDevice> findByIdAndUserId(Long id, Long userId);

    Optional<UserDevice> findByDeviceId(String deviceId);
}

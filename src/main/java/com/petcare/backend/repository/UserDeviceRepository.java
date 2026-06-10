package com.petcare.backend.repository;

import com.petcare.backend.model.UserDevice;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {
    Optional<UserDevice> findByDeviceToken(String deviceToken);
}

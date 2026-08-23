package com.petcare.backend.repository;

import com.petcare.backend.model.UserDevice;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {
    List<UserDevice> findByUserId(Long userId);

    Optional<UserDevice> findByIdAndUserId(Long id, Long userId);

    Optional<UserDevice> findByDeviceId(String deviceId);

    Optional<UserDevice> findByDeviceToken(String deviceToken);

    default Optional<UserDevice> findForRegistration(String deviceId, String deviceToken) {
        Optional<UserDevice> existingByDeviceId = findByDeviceId(deviceId);
        if (existingByDeviceId.isPresent() || deviceToken == null) {
            return existingByDeviceId;
        }
        return findByDeviceToken(deviceToken);
    }

    @Query("""
            select d
            from UserDevice d
            join d.user u
            where u.id = :userId
              and u.pushNotificationEnabled = true
              and d.notificationEnabled = true
              and d.deviceToken is not null
              and d.deviceToken <> ''
            """)
    List<UserDevice> findPushEnabledDevicesByUserId(@Param("userId") Long userId);
}

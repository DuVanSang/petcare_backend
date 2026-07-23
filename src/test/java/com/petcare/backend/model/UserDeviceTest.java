package com.petcare.backend.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class UserDeviceTest {
    @Test
    void accessorsDefaultsAndLifecycle_TrackDeviceAndActivityDates() {
        UserDevice device = new UserDevice(); User user = new User(); user.setId(1L);
        device.setId(2L); device.setUser(user); device.setDeviceId("device"); device.setDeviceName(""); device.setDeviceType("ios");
        device.setDeviceToken(null); device.setNotificationEnabled(false); device.setAppVersion("1.0"); device.setOsVersion("18");
        assertThat(device.getNotificationEnabled()).isFalse(); assertThat(device.getUser()).isSameAs(user);
        device.prePersist();
        assertThat(device.getCreatedAt()).isNotNull(); assertThat(device.getUpdatedAt()).isNotNull();
        assertThat(device.getLastActiveAt()).isNotNull(); assertThat(device.getLastLoginAt()).isNotNull();
        LocalDateTime beforeUpdate = device.getUpdatedAt(); device.preUpdate();
        assertThat(device.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);
    }
}

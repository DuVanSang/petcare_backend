package com.petcare.backend.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.petcare.backend.model.Notification;
import org.junit.jupiter.api.Test;

class NoOpPushNotificationSenderTest {
    @Test
    void sendAcceptsNotificationAndNullWithoutSideEffects() {
        NoOpPushNotificationSender sender = new NoOpPushNotificationSender();

        assertDoesNotThrow(() -> sender.send(new Notification()));
        assertDoesNotThrow(() -> sender.send(null));
    }
}

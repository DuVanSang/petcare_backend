package com.petcare.backend.service.impl;

import com.petcare.backend.model.Notification;
import com.petcare.backend.service.PushNotificationSender;
import org.springframework.stereotype.Component;

@Component
public class NoOpPushNotificationSender implements PushNotificationSender {
    @Override
    public void send(Notification notification) {
        // Firebase push will be connected through this adapter later.
    }
}

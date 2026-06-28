package com.petcare.backend.service;

import com.petcare.backend.model.Notification;

public interface PushNotificationSender {
    void send(Notification notification);
}

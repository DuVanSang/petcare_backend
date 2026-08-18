package com.petcare.backend.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.petcare.backend.model.Notification;
import com.petcare.backend.model.User;
import com.petcare.backend.model.UserDevice;
import com.petcare.backend.repository.UserDeviceRepository;
import com.petcare.backend.service.PushNotificationSender;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.firebase.enabled", havingValue = "true")
public class FirebasePushNotificationSender implements PushNotificationSender {
    private static final TypeReference<Map<String, Object>> DATA_TYPE = new TypeReference<>() {};

    private final FirebaseMessaging firebaseMessaging;
    private final UserDeviceRepository userDeviceRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void send(Notification notification) {
        if (notification == null || notification.getReceiver() == null) {
            return;
        }

        User receiver = notification.getReceiver();
        String type = notification.getType();
        
        // If it's a reminder notification, check if the user has disabled reminders
        boolean isReminder = type != null && (type.contains("reminder") || type.equals("vaccination_consultation"));
        if (isReminder && Boolean.FALSE.equals(receiver.getReminderAlertsEnabled())) {
            log.info("Bỏ qua gửi push notification loại {} vì user {} đã tắt nhắc nhở", type, receiver.getId());
            return;
        }

        List<UserDevice> devices = userDeviceRepository.findPushEnabledDevicesByUserId(
                notification.getReceiver().getId()
        );

        for (UserDevice device : devices) {
            sendToDevice(notification, device);
        }
    }

    private void sendToDevice(Notification notification, UserDevice device) {
        String token = device.getDeviceToken();
        if (!StringUtils.hasText(token)) {
            return;
        }

        if (token.startsWith("ExponentPushToken")) {
            log.warn("Bỏ qua Expo push token của device {} vì Firebase cần native FCM token", device.getId());
            return;
        }

        try {
            firebaseMessaging.send(buildMessage(notification, token));
        } catch (FirebaseMessagingException ex) {
            log.warn(
                    "Không gửi được Firebase push cho device {}: {} - {}",
                    device.getId(),
                    ex.getMessagingErrorCode(),
                    ex.getMessage()
            );
            disableInvalidTokenIfNeeded(device, ex);
        }
    }

    private Message buildMessage(Notification notification, String token) {
        return Message.builder()
                .setToken(token)
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(notification.getTitle())
                        .setBody(notification.getBody())
                        .build())
                .putAllData(buildData(notification))
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(AndroidNotification.builder()
                                .setSound("default")
                                .setClickAction("PETCARE_NOTIFICATION")
                                .setIcon("notification_icon")
                                .setColor("#1E90FF")
                                .build())
                        .build())
                .setApnsConfig(ApnsConfig.builder()
                        .setAps(Aps.builder()
                                .setSound("default")
                                .build())
                        .build())
                .build();
    }

    private Map<String, String> buildData(Notification notification) {
        Map<String, String> data = new HashMap<>();
        data.put("notificationId", String.valueOf(notification.getId()));
        data.put("type", notification.getType());

        if (StringUtils.hasText(notification.getData())) {
            try {
                Map<String, Object> parsed = objectMapper.readValue(notification.getData(), DATA_TYPE);
                parsed.forEach((key, value) -> {
                    if (key != null && value != null) {
                        data.put(key, String.valueOf(value));
                    }
                });
            } catch (Exception ex) {
                log.warn("Không đọc được data JSON của notification {}", notification.getId());
            }
        }

        return data;
    }

    private void disableInvalidTokenIfNeeded(UserDevice device, FirebaseMessagingException ex) {
        if (ex.getMessagingErrorCode() == null) {
            return;
        }

        switch (ex.getMessagingErrorCode()) {
            case INVALID_ARGUMENT, UNREGISTERED, SENDER_ID_MISMATCH -> {
                device.setNotificationEnabled(false);
                userDeviceRepository.save(device);
            }
            default -> {
            }
        }
    }
}

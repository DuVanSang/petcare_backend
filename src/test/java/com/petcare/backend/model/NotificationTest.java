package com.petcare.backend.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class NotificationTest {
    @Test
    void lifecycle_AppliesReadAndSentDefaultsAndPreservesExplicitValues() {
        Notification sent = new Notification();
        User receiver = new User(); receiver.setId(1L);
        sent.setId(2L); sent.setReceiver(receiver); sent.setSender(null); sent.setTitle("Title"); sent.setBody("Body");
        sent.setData("{}"); sent.setType("system"); sent.setStatus("SENT"); sent.setIsRead(null); sent.setScheduledAt(null);
        sent.prePersist();
        assertThat(sent.getIsRead()).isFalse(); assertThat(sent.getSentAt()).isNotNull();
        assertThat(sent.getCreatedAt()).isNotNull(); assertThat(sent.getUpdatedAt()).isNotNull();
        LocalDateTime sentAt = sent.getSentAt(); LocalDateTime beforeUpdate = sent.getUpdatedAt(); sent.preUpdate();
        assertThat(sent.getSentAt()).isEqualTo(sentAt); assertThat(sent.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);

        Notification pending = new Notification(); pending.setStatus("pending"); pending.setIsRead(true);
        LocalDateTime existingSentAt = LocalDateTime.of(2025, 1, 1, 0, 0); pending.setSentAt(existingSentAt); pending.prePersist();
        assertThat(pending.getIsRead()).isTrue(); assertThat(pending.getSentAt()).isEqualTo(existingSentAt);
    }
}

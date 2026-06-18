package com.petcare.backend.model.enums;

import java.util.Arrays;

public enum FriendRequestStatus {
    PENDING("pending"),
    ACCEPTED("accepted"),
    DECLINED("declined"),
    CANCELLED("cancelled");

    private final String value;

    FriendRequestStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static FriendRequestStatus fromValue(String value) {
        String normalizedValue = value == null ? null : value.trim();
        return Arrays.stream(values())
                .filter(status -> status.value.equalsIgnoreCase(normalizedValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid friend request status: " + value));
    }
}

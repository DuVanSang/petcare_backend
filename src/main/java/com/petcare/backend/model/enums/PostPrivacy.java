package com.petcare.backend.model.enums;

import java.util.Arrays;

public enum PostPrivacy {
    PUBLIC("public"),
    FOLLOWERS("followers"),
    PRIVATE("private");

    private final String value;

    PostPrivacy(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static PostPrivacy fromValue(String value) {
        String normalizedValue = value == null ? null : value.trim();
        return Arrays.stream(values())
                .filter(privacy -> privacy.value.equalsIgnoreCase(normalizedValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid post privacy: " + value));
    }
}

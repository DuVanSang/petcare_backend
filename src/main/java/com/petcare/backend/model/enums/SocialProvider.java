package com.petcare.backend.model.enums;

import java.util.Arrays;

public enum SocialProvider {
    GOOGLE("google"),
    APPLE("apple");

    private final String value;

    SocialProvider(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static SocialProvider fromValue(String value) {
        return Arrays.stream(values())
                .filter(provider -> provider.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid social provider: " + value));
    }
}

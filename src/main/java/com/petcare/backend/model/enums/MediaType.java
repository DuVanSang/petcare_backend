package com.petcare.backend.model.enums;

import java.util.Arrays;

public enum MediaType {
    IMAGE("image"),
    VIDEO("video"),
    DOCUMENT("document"),
    OTHER("other");

    private final String value;

    MediaType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static MediaType fromValue(String value) {
        String normalizedValue = value == null ? null : value.trim();
        return Arrays.stream(values())
                .filter(mediaType -> mediaType.value.equalsIgnoreCase(normalizedValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid media type: " + value));
    }
}

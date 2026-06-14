package com.petcare.backend.model.enums;

import java.util.Arrays;

public enum CommentStatus {
    VISIBLE("visible"),
    HIDDEN("hidden"),
    DELETED("deleted");

    private final String value;

    CommentStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static CommentStatus fromValue(String value) {
        String normalizedValue = value == null ? null : value.trim();
        return Arrays.stream(values())
                .filter(status -> status.value.equalsIgnoreCase(normalizedValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid comment status: " + value));
    }
}

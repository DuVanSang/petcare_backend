package com.petcare.backend.model.enums;

public enum PostPrivacy {
    PUBLIC("public"),
    FRIENDS("friends"),
    PRIVATE("private");

    private final String value;

    PostPrivacy(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static PostPrivacy fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Post privacy is required");
        }

        String normalizedValue = value.trim();
        if ("followers".equalsIgnoreCase(normalizedValue)) {
            return FRIENDS;
        }

        for (PostPrivacy privacy : values()) {
            if (privacy.value.equalsIgnoreCase(normalizedValue)) {
                return privacy;
            }
        }
        throw new IllegalArgumentException("Invalid post privacy: " + value);
    }
}

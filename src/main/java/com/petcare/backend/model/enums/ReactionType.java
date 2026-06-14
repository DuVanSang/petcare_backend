package com.petcare.backend.model.enums;

import java.util.Arrays;

public enum ReactionType {
    LIKE("like"),
    LOVE("love"),
    HAHA("haha"),
    WOW("wow"),
    SAD("sad"),
    ANGRY("angry"),
    CARE("care");

    private final String value;

    ReactionType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ReactionType fromValue(String value) {
        String normalizedValue = value == null ? null : value.trim();
        return Arrays.stream(values())
                .filter(reactionType -> reactionType.value.equalsIgnoreCase(normalizedValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid reaction type: " + value));
    }
}

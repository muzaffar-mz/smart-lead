package com.muzaffar.smartlead.domain.entity.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum LeadUrgency {
    HIGH("high"),
    MEDIUM("medium"),
    LOW("low");

    private final String value;

    LeadUrgency(String value) {
        this.value = value;
    }

    public static LeadUrgency fromValue(String value) {
        return Arrays.stream(values())
                .filter(u -> u.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown lead urgency: " + value));
    }
}

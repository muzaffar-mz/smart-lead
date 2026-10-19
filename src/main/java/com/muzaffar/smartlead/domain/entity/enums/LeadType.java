package com.muzaffar.smartlead.domain.entity.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum LeadType {
    DEMO_REQUEST("demo-request"),
    PRICING_INQUIRY("pricing-inquiry"),
    PARTNERSHIP("partnership"),
    SUPPORT("support"),
    OTHER("other");

    private final String value;

    LeadType(String value) {
        this.value = value;
    }

    public static LeadType fromValue(String value) {
        return Arrays.stream(values())
                .filter(t -> t.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown lead type: " + value));
    }
}

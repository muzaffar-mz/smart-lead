package com.muzaffar.smartlead.exceptions;

import org.springframework.http.HttpHeaders;

import java.util.regex.Pattern;

public record RateLimitInfo(
        Long retryAfterMillis,
        Long limitRequestsPerDay,
        Long limitTokensPerMinute,
        Long remainingRequestsPerDay,
        Long remainingTokensPerMinute,
        Long resetRequestsPerDayMillis,
        Long resetTokensPerMinuteMillis
) {

    private static final Pattern DURATION_PART = Pattern.compile("(\\d+(?:\\.\\d+)?)(ms|d|h|m|s)");

    public static RateLimitInfo from(HttpHeaders headers) {
        return new RateLimitInfo(
                parseSecondsAsMillis(headers.getFirst("retry-after")),
                parseLong(headers.getFirst("x-ratelimit-limit-requests")),
                parseLong(headers.getFirst("x-ratelimit-limit-tokens")),
                parseLong(headers.getFirst("x-ratelimit-remaining-requests")),
                parseLong(headers.getFirst("x-ratelimit-remaining-tokens")),
                parseDurationMillis(headers.getFirst("x-ratelimit-reset-requests")),
                parseDurationMillis(headers.getFirst("x-ratelimit-reset-tokens")));
    }

    private static Long parseSecondsAsMillis(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Math.round(Double.parseDouble(value.trim()) * 1_000);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long parseLong(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Converts durations like "2m59.56s", "7.66s" or "250ms" to milliseconds.
     */
    static Long parseDurationMillis(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        var matcher = DURATION_PART.matcher(value.trim());
        double total = 0;
        boolean found = false;
        while (matcher.find()) {
            found = true;
            total += Double.parseDouble(matcher.group(1)) * unitMillis(matcher.group(2));
        }
        return found ? Math.round(total) : null;
    }

    private static double unitMillis(String unit) {
        return switch (unit) {
            case "ms" -> 1;
            case "s" -> 1_000;
            case "m" -> 60_000;
            case "h" -> 3_600_000;
            default -> 86_400_000; // d
        };
    }
}

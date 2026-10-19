package com.muzaffar.smartlead.service.message.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;


public record MessageFilter(
        @Schema(description = "Message id", example = "1") Long id,
        @Schema(description = "Part of the email, ignoring case", example = "acme.com")
        @Size(max = 255, message = "email must be at most 255 characters") String email,
        @Schema(description = "Part of the phone number; spaces, dashes, dots and parentheses are ignored", example = "555")
        @Size(max = 50, message = "phone must be at most 50 characters") String phone,
        @Schema(description = "Part of the IP address", example = "192.168")
        @Size(max = 45, message = "ip must be at most 45 characters") String ip,
        @Schema(description = "Created at or after this moment (ISO date-time). Default: 30 days before 'to'",
                example = "2026-10-01T00:00:00")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @Schema(description = "Created at or before this moment (ISO date-time). Default: now",
                example = "2026-10-31T23:59:59")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
        @Schema(description = "Page number, starting at 0", defaultValue = "0", example = "0")
        @Min(value = 0, message = "page must be 0 or greater") Integer page,
        @Schema(description = "Page size", defaultValue = "100", example = "100", minimum = "1", maximum = "5000")
        @Min(value = 1, message = "size must be between 1 and 5000")
        @Max(value = 5000, message = "size must be between 1 and 5000") Integer size,
        @Schema(description = "Field to sort by", defaultValue = "id", allowableValues = {"id", "dateCreated", "email", "phone", "ip"})
        @Pattern(regexp = "id|dateCreated|email|phone|ip",
                message = "sortBy must be one of: id, dateCreated, email, phone, ip") String sortBy,
        @Schema(description = "Sort direction", defaultValue = "asc", allowableValues = {"asc", "desc"})
        @Pattern(regexp = "(?i)asc|desc", message = "direction must be asc or desc") String direction
) {

    private static final int DEFAULT_RANGE_DAYS = 30;

    public MessageFilter {
        if (from == null && to == null) {
            to = LocalDateTime.now();
            from = to.minusDays(DEFAULT_RANGE_DAYS);
        } else if (from == null) {
            from = to.minusDays(DEFAULT_RANGE_DAYS);
        } else if (to == null) {
            to = LocalDateTime.now();
        }
        page = page == null ? 0 : page;
        size = size == null ? 100 : size;
        sortBy = sortBy == null ? "id" : sortBy;
        direction = direction == null ? "asc" : direction;
    }

    public Pageable toPageable() {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sortBy));
    }
}

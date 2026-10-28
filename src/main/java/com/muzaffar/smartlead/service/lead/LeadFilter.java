package com.muzaffar.smartlead.service.lead;

import com.muzaffar.smartlead.domain.entity.enums.LeadType;
import com.muzaffar.smartlead.domain.entity.enums.LeadUrgency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public record LeadFilter(
        @Schema(description = "Only leads of this type", allowableValues = {"DEMO_REQUEST", "PRICING_INQUIRY", "PARTNERSHIP", "SUPPORT", "OTHER"}) LeadType type,
        @Schema(description = "Only leads with this urgency", allowableValues = {"HIGH", "MEDIUM", "LOW"}) LeadUrgency urgency,
        @Schema(description = "Only leads created from this message id", example = "1") Long messageId,
        @Schema(description = "Created at or after this moment (ISO date-time)", example = "2026-10-01T00:00:00")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @Schema(description = "Created at or before this moment (ISO date-time)", example = "2026-10-31T23:59:59")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
        @Schema(description = "Page number, starting at 0", defaultValue = "0", example = "0")
        @Min(value = 0, message = "page must be 0 or greater") Integer page,
        @Schema(description = "Page size", defaultValue = "100", example = "100", minimum = "1", maximum = "5000")
        @Min(value = 1, message = "size must be between 1 and 5000")
        @Max(value = 5000, message = "size must be between 1 and 5000") Integer size,
        @Schema(description = "Field to sort by", defaultValue = "id", allowableValues = {"id", "dateCreated", "type", "urgency"})
        @Pattern(regexp = "id|dateCreated|type|urgency",
                message = "sortBy must be one of: id, dateCreated, type, urgency") String sortBy,
        @Schema(description = "Sort direction", defaultValue = "asc", allowableValues = {"asc", "desc"})
        @Pattern(regexp = "(?i)asc|desc", message = "direction must be asc or desc") String direction
) {

    public LeadFilter {
        page = page == null ? 0 : page;
        size = size == null ? 100 : size;
        sortBy = sortBy == null ? "id" : sortBy;
        direction = direction == null ? "asc" : direction;
    }

    public Pageable toPageable() {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sortBy));
    }
}

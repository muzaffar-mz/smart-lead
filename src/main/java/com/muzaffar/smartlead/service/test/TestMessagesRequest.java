package com.muzaffar.smartlead.service.test;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record TestMessagesRequest(
        @Schema(description = "How many messages to submit. Texts are random phrases of test-phrases.txt; none repeats until all were used",
                defaultValue = "10", example = "10", minimum = "1", maximum = "1000")
        @Min(value = 1, message = "count must be between 1 and 1000")
        @Max(value = 1000, message = "count must be between 1 and 1000") Integer count
) {

    public TestMessagesRequest {
        count = count == null ? 10 : count;
    }
}

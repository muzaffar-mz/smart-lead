package com.muzaffar.smartlead.service.completion.model;

import com.muzaffar.smartlead.domain.entity.UsageDetails;
import io.swagger.v3.oas.annotations.media.Schema;

public record CompletionUsageDTO(
        @Schema(description = "Seconds the request waited in the provider queue") Double queueTime,
        @Schema(description = "Tokens in the prompt") Integer promptTokens,
        @Schema(description = "Seconds spent processing the prompt") Double promptTime,
        @Schema(description = "Tokens in the completion") Integer completionTokens,
        @Schema(description = "Seconds spent generating the completion") Double completionTime,
        @Schema(description = "Prompt plus completion tokens") Integer totalTokens,
        @Schema(description = "Total seconds spent on the request") Double totalTime,
        @Schema(description = "Tokens used for reasoning") Integer reasoningTokens
) {

    public static CompletionUsageDTO from(UsageDetails entity) {
        if (entity == null) {
            return null;
        }
        return new CompletionUsageDTO(
                entity.getQueueTime(),
                entity.getPromptTokens(),
                entity.getPromptTime(),
                entity.getCompletionTokens(),
                entity.getCompletionTime(),
                entity.getTotalTokens(),
                entity.getTotalTime(),
                entity.getReasoningTokens());
    }
}

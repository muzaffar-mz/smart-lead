package com.muzaffar.smartlead.service.completion;

import com.muzaffar.smartlead.domain.entity.HfCompletion;
import com.muzaffar.smartlead.domain.entity.enums.CompletionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record CompletionDTO(
        @Schema(description = "Completion id", example = "1") Long id,
        @Schema(description = "Id of the message that was classified", example = "1") Long messageId,
        @Schema(description = "PENDING until the model answered, then SUCCESS or ERROR") CompletionStatus status,
        @Schema(description = "Text that was sent to the model (on ERROR, the error message)") String message,
        @Schema(description = "Model that produced the answer", example = "openai/gpt-oss-120b:groq") String model,
        @Schema(description = "Role of the answer", example = "assistant") String role,
        @Schema(description = "Raw answer of the model") String content,
        @Schema(description = "Reasoning of the model, if returned") String reasoning,
        @Schema(description = "Token usage and timings, if returned") CompletionUsageDTO usage,
        LocalDateTime dateCreated,
        LocalDateTime dateUpdated
) {

    public static CompletionDTO from(HfCompletion entity) {
        return new CompletionDTO(
                entity.getId(),
                entity.getMessageId(),
                entity.getStatus(),
                entity.getMessage(),
                entity.getModel(),
                entity.getRole(),
                entity.getContent(),
                entity.getReasoning(),
                CompletionUsageDTO.from(entity.getUsage()),
                entity.getDateCreated(),
                entity.getDateUpdated());
    }
}

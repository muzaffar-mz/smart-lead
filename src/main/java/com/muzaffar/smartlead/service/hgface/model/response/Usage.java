package com.muzaffar.smartlead.service.hgface.model.response;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record Usage(
        Double queueTime,
        Integer promptTokens,
        Double promptTime,
        Integer completionTokens,
        Double completionTime,
        Integer totalTokens,
        Double totalTime,
        CompletionTokensDetails completionTokensDetails
) {


    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CompletionTokensDetails(
            Integer reasoningTokens
    ) {
    }

}

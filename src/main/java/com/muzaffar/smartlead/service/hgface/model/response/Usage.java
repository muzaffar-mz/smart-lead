package com.muzaffar.smartlead.service.hgface.model.response;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record Usage(
        double queueTime,
        int promptTokens,
        double promptTime,
        int completionTokens,
        double completionTime,
        int totalTokens,
        double totalTime,
        CompletionTokensDetails completionTokensDetails
) {


    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CompletionTokensDetails(
            int reasoningTokens
    ) {
    }

}

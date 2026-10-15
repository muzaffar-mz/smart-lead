package com.muzaffar.smartlead.service.hgface.model.request;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CompletionRequest(
        String model,
        List<RequestMessage> messages,
        Double temperature,
        Integer maxTokens,
        String reasoningEffort,
        Boolean stream,
        ResponseFormat responseFormat
) {
}

package com.muzaffar.smartlead.service.completion.hgfacerest.model.request;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ResponseFormat(
        String type,
        JsonSchema jsonSchema
) {
}

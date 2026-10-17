package com.muzaffar.smartlead.service.hgface.model;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RawLeadClassificationDTO(
        boolean isLead,
        String type,
        String title,
        String urgency,
        String summary
) {
}

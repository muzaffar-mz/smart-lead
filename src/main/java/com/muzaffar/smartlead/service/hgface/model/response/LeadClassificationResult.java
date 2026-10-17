package com.muzaffar.smartlead.service.hgface.model.response;

import com.muzaffar.smartlead.service.hgface.model.RawLeadClassificationDTO;

public record LeadClassificationResult(
        RawLeadClassificationDTO classification,
        String reasoning,
        Usage usage,
        String model,
        String role,
        String content
) {
}

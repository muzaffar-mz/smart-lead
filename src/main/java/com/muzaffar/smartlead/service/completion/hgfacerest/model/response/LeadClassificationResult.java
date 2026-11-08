package com.muzaffar.smartlead.service.completion.hgfacerest.model.response;

import com.muzaffar.smartlead.service.completion.hgfacerest.model.RawLeadClassificationDTO;

public record LeadClassificationResult(
        RawLeadClassificationDTO classification,
        String reasoning,
        Usage usage,
        String model,
        String role,
        String content
) {
}

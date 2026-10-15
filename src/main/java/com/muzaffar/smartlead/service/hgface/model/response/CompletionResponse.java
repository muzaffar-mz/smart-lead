package com.muzaffar.smartlead.service.hgface.model.response;

import java.util.List;

public record CompletionResponse(
        List<Choice> choices,
        Usage usage
) {
}

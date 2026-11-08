package com.muzaffar.smartlead.service.completion.hgfacerest.model.response;

import java.util.List;

public record CompletionResponse(
        List<Choice> choices,
        Usage usage
) {
}

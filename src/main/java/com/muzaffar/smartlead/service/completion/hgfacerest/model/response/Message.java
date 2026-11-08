package com.muzaffar.smartlead.service.completion.hgfacerest.model.response;

public record Message(
        String role,
        String content,
        String reasoning
) {
}

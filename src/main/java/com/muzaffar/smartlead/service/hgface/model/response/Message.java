package com.muzaffar.smartlead.service.hgface.model.response;

public record Message(
        String role,
        String content,
        String reasoning
) {
}

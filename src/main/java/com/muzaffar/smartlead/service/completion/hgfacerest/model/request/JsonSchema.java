package com.muzaffar.smartlead.service.completion.hgfacerest.model.request;

import java.util.Map;

public record JsonSchema(
        String name,
        boolean strict,
        Map<String, Object> schema
) {
}

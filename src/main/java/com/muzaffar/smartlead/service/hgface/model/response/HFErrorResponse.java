package com.muzaffar.smartlead.service.hgface.model.response;


public record HFErrorResponse(
        String message,
        String type,
        String code
) {
}

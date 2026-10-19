package com.muzaffar.smartlead.service.hgface.model.response;


public record HFErrorResponse(
        HFErrorBody error
) {

    public record HFErrorBody(
            String message,
            String type,
            String code
    ){}
}

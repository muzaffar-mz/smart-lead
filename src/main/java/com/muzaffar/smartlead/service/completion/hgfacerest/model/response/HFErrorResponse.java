package com.muzaffar.smartlead.service.completion.hgfacerest.model.response;


public record HFErrorResponse(
        HFErrorBody error
) {

    public record HFErrorBody(
            String message,
            String type,
            String code
    ){}
}

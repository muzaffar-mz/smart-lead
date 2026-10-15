package com.muzaffar.smartlead.exceptions;


import com.muzaffar.smartlead.exceptions.enums.ErrorConstant;
import com.muzaffar.smartlead.service.hgface.model.response.HFErrorResponse;
import lombok.Getter;

@Getter
public class HFException extends BaseException {
    private final String type;
    private final String code;

    public HFException(ErrorConstant errorConstant, HFErrorResponse errorResponse) {
        super(errorConstant, errorResponse.message());
        this.type = errorResponse.type();
        this.code = errorResponse.code();
    }
}

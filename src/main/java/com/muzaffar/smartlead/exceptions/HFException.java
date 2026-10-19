package com.muzaffar.smartlead.exceptions;


import com.muzaffar.smartlead.exceptions.enums.ErrorConstant;
import com.muzaffar.smartlead.service.hgface.model.response.HFErrorResponse;
import lombok.Getter;

@Getter
public class HFException extends BaseException {
    private final String type;
    private final String code;
    private final int httpStatus;
    private final RateLimitInfo rateLimit;

    public HFException(ErrorConstant errorConstant, HFErrorResponse errorResponse, int httpStatus, RateLimitInfo rateLimit) {
        super(errorConstant, errorResponse.error().message());
        this.type = errorResponse.error().type();
        this.code = errorResponse.error().code();
        this.httpStatus = httpStatus;
        this.rateLimit = rateLimit;
    }
}

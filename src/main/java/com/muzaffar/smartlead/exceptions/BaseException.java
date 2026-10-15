package com.muzaffar.smartlead.exceptions;

import com.muzaffar.smartlead.exceptions.enums.ErrorConstant;
import lombok.Getter;

@Getter
public abstract class BaseException extends RuntimeException {
    private final ErrorConstant errorConstant;

    public BaseException(ErrorConstant errorConstant, String message) {
        super(message);
        this.errorConstant = errorConstant;
    }
}
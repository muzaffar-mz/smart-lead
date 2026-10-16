package com.muzaffar.smartlead.exceptions;


import com.muzaffar.smartlead.exceptions.enums.ErrorConstant;
import lombok.Getter;

@Getter
public class NotFoundException extends BaseException {

    public NotFoundException(ErrorConstant errorConstant, String error) {
        super(errorConstant, error);
    }
}

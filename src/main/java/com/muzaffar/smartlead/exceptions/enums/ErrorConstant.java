package com.muzaffar.smartlead.exceptions.enums;


import lombok.Getter;

@Getter
public enum ErrorConstant {

    HUGGING_FACE_EXCEPTION(-1),
    NOT_FOUND(-10),
    INVALID_REQUEST(-20),
    INTERNAL_ERROR(-99),

    ;

    private final int code;

    ErrorConstant(int code) {
        this.code = code;
    }

}

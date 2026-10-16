package com.muzaffar.smartlead.exceptions.enums;


import lombok.Getter;

@Getter
public enum ErrorConstant {

    HUGGING_FACE_EXCEPTION(-1),
    NOT_FOUND(-10),

    ;

    private final int code;

    ErrorConstant(int code) {
        this.code = code;
    }

}

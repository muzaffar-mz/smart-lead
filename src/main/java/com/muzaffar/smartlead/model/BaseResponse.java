package com.muzaffar.smartlead.model;


import com.fasterxml.jackson.annotation.JsonProperty;

public record BaseResponse<T>(
        @JsonProperty("code") Integer code,
        @JsonProperty("message") String message,
        @JsonProperty("payload") T payload,
        @JsonProperty("error_message") String errorMessage) {

    public static <T> BaseResponse<T> payload(T payload) {
        return new BaseResponse<>(200, "Successful", payload, null);
    }

    public static BaseResponse<Void> success() {
        return new BaseResponse<>(200, "Successful", null, null);
    }

    public static BaseResponse<Void> error(int code, String errorMessage) {
        return new BaseResponse<>(code, null, null, errorMessage);
    }
}

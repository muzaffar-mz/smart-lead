package com.muzaffar.smartlead.exceptions;

import com.muzaffar.smartlead.exceptions.enums.ErrorConstant;
import com.muzaffar.smartlead.model.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;


import java.util.stream.Collectors;


@Slf4j
@RestControllerAdvice
public class ControllerExceptionHandler {


    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<Void> handleNotValidParametersException(MethodArgumentNotValidException ex) {
        log.error("Method Argument Not Valid Exception: {}", ex);

        String errorMessage = ex.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage).collect(Collectors.joining(", "));
        log.error("Error messages: {}", errorMessage);

        return BaseResponse.error(ErrorConstant.INVALID_REQUEST.getCode(), errorMessage);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public BaseResponse<Void> handleUnreadable(HttpMessageNotReadableException ex) {
        return BaseResponse.error(ErrorConstant.INVALID_REQUEST.getCode(), "Request body is missing or malformed");
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFoundException.class)
    public BaseResponse<Void> handleNotFoundException(NotFoundException ex) {
        log.error("Not Found Exception: {}", ex.getMessage(), ex);
        return BaseResponse.error(ex.getErrorConstant().getCode(), ex.getMessage());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(BaseException.class)
    public BaseResponse<Void> handleFeignException(BaseException ex) {
        log.error("Unhandled Base Exception: {}", ex.getMessage(), ex);
        return BaseResponse.error(ex.getErrorConstant().getCode(), ex.getMessage());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public BaseResponse<Void> handleOtherExceptions(Exception ex) {
        log.error("Unexpected Internal  error: {}", ex.getMessage(), ex);
        return BaseResponse.error(ErrorConstant.INTERNAL_ERROR.getCode(), ex.getMessage());
    }


}

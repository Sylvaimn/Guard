package com.temporedata.guard.common.exception;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.base.exceptions.ZyException;
import com.temporedata.guard.api.base.pojos.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler: translates exceptions into unified BaseResponse.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<Void> handleBusiness(BusinessException e) {
        return BaseResponse.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(ZyException.class)
    public BaseResponse<Void> handleZy(ZyException e) {
        return BaseResponse.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public BaseResponse<Void> handleAccessDenied(AccessDeniedException e) {
        return BaseResponse.error(403, "无权限访问");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public BaseResponse<Void> handleOther(Exception e) {
        log.error("Unhandled exception", e);
        return BaseResponse.error(500, "系统异常: " + e.getMessage());
    }
}
package com.jackqiu.jackpao.exception;

import com.fasterxml.jackson.databind.ser.Serializers;
import com.jackqiu.jackpao.common.BaseResponse;
import com.jackqiu.jackpao.common.ErrorCode;
import com.jackqiu.jackpao.common.ResultUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * @author jackqiu
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse businessExceptionHandler(BusinessException e) {
        log.error("businessException: " + e.getMessage(), e);
        return ResultUtil.error(e.getCode(), e.getMessage(), e.getDescription());
    }

    @ExceptionHandler(Exception.class)
    public BaseResponse ExceptionHandler(Exception e) {
        log.error("RuntimeException: " + e.getMessage(), e);
        return ResultUtil.error(ErrorCode.SYSTEM_ERROR, e.getMessage(), "系统错误");
    }
}

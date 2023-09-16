package com.jackqiu.jackpao.exception;

import com.jackqiu.jackpao.common.ErrorCode;

/**
 * 自定义异常类
 * @author jackqiu
 */
public class BusinessException extends RuntimeException {

    private final Integer code;

    private final String description;

    public BusinessException(String message,Integer code, String description) {
        super(message);
        this.code = code;
        this.description = description;
    }

    public BusinessException(ErrorCode errorCode, String description) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.description = description;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.description = errorCode.getDescription();
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}

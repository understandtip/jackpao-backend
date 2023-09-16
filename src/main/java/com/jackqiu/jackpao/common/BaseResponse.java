package com.jackqiu.jackpao.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用返回类
 * @author jackqiu
 */
@Data
public class BaseResponse<T> implements Serializable {

    private Integer code;

    private T data;

    private String message;

    private String description;

    public BaseResponse(Integer code, T data, String message, String description) {
        this.code = code;
        this.data = data;
        this.message = message;
        this.description = description;
    }

    public BaseResponse(Integer code, T data) {
        this(code,data,"","");
    }

    public BaseResponse(Integer code, T data, String message) {
        this(code,data,message,"");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(),null,errorCode.getMessage(),errorCode.getDescription());
    }
}

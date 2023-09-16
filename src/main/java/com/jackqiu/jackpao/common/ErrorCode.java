package com.jackqiu.jackpao.common;

/**
 * 全局错误状态码
 * @author jackqiu
 */
public enum ErrorCode {
    SUCCESS(0,"成功"),
    PARAM_ERROR(40000,"请求参数错误"),
    NULL_ERROR(40001,"请求数据为空"),
    NO_LOGIN(40100,"未登录"),
    NO_AUTH(40101,"无权限"),
    FORBIDDEN(40300,"禁止操作"),
    SYSTEM_ERROR(40000,"系统内部错误");

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误信息
     */
    private final String message;

    /**
     * 错误描述
     */
    private final String description;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
        this.description = "";
    }

    ErrorCode(Integer code, String message, String description) {
        this.code = code;
        this.message = message;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getDescription() {
        return description;
    }
}

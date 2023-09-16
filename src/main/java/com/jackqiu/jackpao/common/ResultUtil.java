package com.jackqiu.jackpao.common;

import org.apache.poi.ss.formula.functions.T;

/**
 * 响应工具类
 *
 * @author jackqiu
 */
public class ResultUtil {

    /**
     * 成功
     *
     * @param data
     * @param <T>
     * @return
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<T>(0, data, "ok");
    }

    /**
     * 失败
     *
     * @param errorCode
     * @return
     */
    public static BaseResponse error(ErrorCode errorCode) {
        return new BaseResponse(errorCode);
    }

    /**
     * 失败
     * @param errorCode
     * @param message
     * @param description
     * @return
     */
    public static BaseResponse error(ErrorCode errorCode, String message, String description) {
        return new BaseResponse(errorCode.getCode(), null, message, description);
    }

    /**
     * 失败
     * @param code
     * @param message
     * @param description
     * @return
     */
    public static BaseResponse error(Integer code, String message, String description) {
        return new BaseResponse(code, null, message, description);
    }

    /**
     * 失败
     *
     * @param errorCode
     * @param message
     * @return
     */
    public static BaseResponse error(ErrorCode errorCode, String message) {
        return new BaseResponse(errorCode.getCode(), null, message);
    }
}

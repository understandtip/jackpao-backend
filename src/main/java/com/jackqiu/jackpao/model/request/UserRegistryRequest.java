package com.jackqiu.jackpao.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录请求封装类
 *
 * @author jackqiu
 */
@Data
public class UserRegistryRequest implements Serializable {

    private static final long serialVersionUID = 9071951117009220064L;
    /**
     * 账号
     */
    private String userAccount;
    /**
     * 密码
     */
    private String userPassword;
    /**
     * 确认密码
     */
    private String checkPassword;
    /**
     * 星球编号
     */
//    private String planetCode;
}

package com.jackqiu.jackpao.model.enums;

/**
 * @author jackqiu
 */
public enum UserRoleEnum {
    USER(0,"用户"),
    ADMIN(1,"管理员"),
    BAN(2,"被封号");
    private Integer code;

    private String text;

    UserRoleEnum(Integer code, String text) {
        this.code = code;
        this.text = text;
    }

    public static UserRoleEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        UserRoleEnum[] values = UserRoleEnum.values();
        for (UserRoleEnum userRoleEnum : values) {
            if (userRoleEnum.getCode().equals(value)) {
                return userRoleEnum;
            }
        }
        return null;
    }

    public Integer getCode() {
        return code;
    }

    public String getText() {
        return text;
    }
}

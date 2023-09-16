package com.jackqiu.jackpao.model.enums;

/**
 * @author jackqiu
 */
public enum TeamStatusEnum {
    PUBLIC(0,"公开"),
    PRIVATE(1,"私有"),
    SECRET(2,"加密");
    private Integer code;

    private String text;

    TeamStatusEnum(Integer code, String text) {
        this.code = code;
        this.text = text;
    }

    public static TeamStatusEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        TeamStatusEnum[] values = TeamStatusEnum.values();
        for (TeamStatusEnum teamStatusEnum : values) {
            if (teamStatusEnum.getCode().equals(value)) {
                return teamStatusEnum;
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

package com.jackqiu.jackpao.common;

/**
 * @author jackqiu
 */
public enum KeyNameEnum {
    SYSTEM_NAME("jackpao","伙伴匹配系统"),
    USER_MODEL("user","用户模块"),
    TEAM_MODEL("team","队伍模块");

    private final String description;

    private final String name;

    KeyNameEnum(String name, String description) {
        this.description = description;
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }
}

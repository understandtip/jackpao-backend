package com.jackqiu.jackpao.constant;

/**
 * 锁常量
 *
 *  
 */
public interface LockConstant {

    /**
     * 缓存预热的锁
     */
    String precacheJobLock = ":precachejob:docache:lock";

    /**
     * 加入队伍的锁
     */
    String joinTeamLock = ":join_team:lock";
}

package com.jackqiu.jackpao.constant;

/**
 * redis中的健值常量
 * @author jackqiu
 */
public interface RedisKeyConstant {

    /**
     * 缓存预热的锁
     */
    String  LOCATION_MATCH = "jackpao:user:location:match";
}

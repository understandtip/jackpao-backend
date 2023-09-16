package com.jackqiu.jackpao.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jackqiu.jackpao.common.ResultUtil;
import com.jackqiu.jackpao.model.domain.User;
import com.jackqiu.jackpao.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.jackqiu.jackpao.common.KeyNameEnum.SYSTEM_NAME;
import static com.jackqiu.jackpao.common.KeyNameEnum.USER_MODEL;

/**
 * 定时执行缓存的类
 *
 * @author jackqiu
 */
@Component
@Slf4j
public class doCacheJob {

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedissonClient redissonClient;

    // 重点用户
    private List<Long> mainUserList = Arrays.asList(1L);

    @Scheduled(cron = "0 09 21 * * *")
    public void doJob() {
        //获取锁对象
        RLock lock = redissonClient.getLock(SYSTEM_NAME + ":precachejob:docache:lock");
        try {
            //只有一个服务器能够加锁
            if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                System.out.println("getLock: " + Thread.currentThread().getId());
                for (Long userId : mainUserList) {
                    String redisKey = SYSTEM_NAME + ":" + USER_MODEL + ":recommend:" + userId;
                    String redisKey2 = String.format("%s:%s:match:%s", SYSTEM_NAME, USER_MODEL, userId);
                    ValueOperations<String, Object> ops = redisTemplate.opsForValue();
                    //1.查询数据库
                    QueryWrapper<User> queryWrapper = new QueryWrapper<>();
                    Page<User> userPage = userService.page(new Page<>(1, 8), queryWrapper);
                    List<User> userList = userService.matchUsers(10, userService.getById(userId));
                    //2.查询出来之后，将数据保存在缓存中
                    try {
                        ops.set(redisKey, userPage, 23, TimeUnit.HOURS);
                        ops.set(redisKey2, userList, 23, TimeUnit.HOURS);
                    } catch (Exception e) {
                        log.error("redis set key error", e);
                    }
                }
            }
        } catch (InterruptedException e) {
            log.error("doCacheRecommendUser error", e);
        } finally {
            //如果是自己加的锁，才有资格释放
            if (lock.isHeldByCurrentThread()) {
                System.out.println("unLock: " + Thread.currentThread().getId());
                lock.unlock();
            }
        }
    }
}

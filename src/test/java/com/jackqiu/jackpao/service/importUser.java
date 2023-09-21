package com.jackqiu.jackpao.service;

import com.jackqiu.jackpao.model.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author jackqiu
 */

@SpringBootTest
public class importUser {

    @Resource
    private UserService userService;

    private ExecutorService executorService = new ThreadPoolExecutor(400,600,5, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1000));

    @Test
    public void doInsertUsers() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int INSERT_NUM = 100;
        List<User> userList = new ArrayList<>();
        for (int i = 0; i < INSERT_NUM; i++) {
            User user = new User();
            user.setUsername("jackqiu");
            user.setUserAccount("fakejackqiu");
            user.setAvatarUrl("https://img2.baidu.com/it/u=2428398243,1290416697&fm=253&fmt=auto&app=138&f=JPEG?w=514&h=500");
            user.setGender(1);
            user.setUserPassword("12345678");
            user.setPhone("123");
            user.setEmail("123@qq.com");
            user.setTags("[\"java\"]");
            user.setUserStatus(0);
            user.setUserRole(0);
            user.setPlanetCode("11111111");
            userList.add(user);
        }
        // 20 秒 10 万条
        userService.saveBatch(userList, 10000);
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
    }

    @Test
    public void doInsertUsersByBatch() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        int j = 0;
        final int INSERT_NUM = 10000;
        List<CompletableFuture<Void>> futureList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            List<User> userList = new ArrayList<>();
            while (true) {
                j++;
                User user = new User();
                user.setUsername("假鱼皮");
                user.setUserAccount("fakeyupi");
                user.setAvatarUrl("https://636f-codenav-8grj8px727565176-1256524210.tcb.qcloud.la/img/logo.png");
                user.setGender(0);
                user.setUserPassword("12345678");
                user.setPhone("123");
                user.setEmail("123@qq.com");
                user.setTags("[]");
                user.setUserStatus(0);
                user.setUserRole(0);
                user.setPlanetCode("11111111");
                userList.add(user);
                if (j % INSERT_NUM == 0) {
                    break;
                }
            }
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                System.out.println("threadName: " + Thread.currentThread().getName());
                userService.saveBatch(userList, INSERT_NUM);
            }, executorService);
            futureList.add(future);
        }
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[]{})).join();
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
    }
}

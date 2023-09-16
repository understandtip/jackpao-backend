package com.jackqiu.jackpao;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author jackqiu
 */
@SpringBootApplication
@MapperScan("com.jackqiu.jackpao.mapper")
@EnableScheduling
public class JackpaoBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(JackpaoBackendApplication.class, args);
    }

}

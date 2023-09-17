package com.jackqiu.jackpao.mapper;

import com.jackqiu.jackpao.model.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TeamMapperTest {

    @Autowired
    private TeamMapper teamMapper;

    @Test
    void associatedUsers() {
        for (User user : teamMapper.associatedUsers(2L)) {
            System.out.println(user);
        }

    }
}
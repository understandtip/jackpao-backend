package com.jackqiu.jackpao.mapper;

import com.jackqiu.jackpao.model.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

import static com.jackqiu.jackpao.constant.RedisKeyConstant.LOCATION_MATCH;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TeamMapperTest {

    @Autowired
    private TeamMapper teamMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void associatedUsers() {
        for (User user : teamMapper.associatedUsers(2L)) {
            System.out.println(user);
        }

    }

    @Test
    void name() {
        GeoOperations<String, Object> opsForGeo = redisTemplate.opsForGeo();
//        opsForGeo.add(LOCATION_MATCH, new Point(115.86458944,30.68945530),"user:2");
        List<Point> position = opsForGeo.position(LOCATION_MATCH,"user:1","user:2");
        opsForGeo.distance(LOCATION_MATCH,"user:1","user:2");
        for (Point point : position) {
            System.out.println(point);
        }
    }
}
package com.jackqiu.jackpao.mapper;

import com.jackqiu.jackpao.model.domain.Team;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jackqiu.jackpao.model.domain.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
* @author jackqiu
* @description 针对表【team(队伍)】的数据库操作Mapper
* @createDate 2023-09-07 14:32:56
* @Entity generator.domain.Team
*/
public interface TeamMapper extends BaseMapper<Team> {

    List<User> associatedUsers(@Param("id") Long id);
}





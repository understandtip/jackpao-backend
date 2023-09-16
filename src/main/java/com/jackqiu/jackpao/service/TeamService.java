package com.jackqiu.jackpao.service;

import com.jackqiu.jackpao.model.domain.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jackqiu.jackpao.model.domain.User;
import com.jackqiu.jackpao.model.request.TeamAddRequest;
import com.jackqiu.jackpao.model.request.TeamQueryRequest;

import java.util.List;

/**
* @author jackqiu
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2023-09-07 14:32:56
*/
public interface TeamService extends IService<Team> {

    /**
     * 添加队伍
     * @param teamAddRequest
     * @param currentUser
     * @return
     */
    Long addTeam(TeamAddRequest teamAddRequest, User currentUser);

    /**
     * 获取队伍分页列表
     * @param teamQueryRequest
     * @param currentUser
     * @return
     */
    List<Team> getTeamList(TeamQueryRequest teamQueryRequest, User currentUser);
}

package com.jackqiu.jackpao.service;

import com.jackqiu.jackpao.model.domain.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jackqiu.jackpao.model.domain.User;
import com.jackqiu.jackpao.model.request.TeamAddRequest;
import com.jackqiu.jackpao.model.request.TeamQueryRequest;
import com.jackqiu.jackpao.model.request.TeamUpdateRequest;
import com.jackqiu.jackpao.model.vo.TeamVO;

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
    List<TeamVO> getTeamList(TeamQueryRequest teamQueryRequest, User currentUser);

    /**
     * 更新队伍信息
     * @param teamUpdateRequest
     * @param currentUser
     * @return
     */
    boolean updateTeam(TeamUpdateRequest teamUpdateRequest,User currentUser);
}

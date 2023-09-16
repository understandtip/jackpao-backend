package com.jackqiu.jackpao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jackqiu.jackpao.model.domain.UserTeam;
import com.jackqiu.jackpao.mapper.UserTeamMapper;
import com.jackqiu.jackpao.service.UserTeamService;
import org.springframework.stereotype.Service;

/**
* @author jackqiu
* @description 针对表【user_team(用户队伍关系)】的数据库操作Service实现
* @createDate 2023-09-07 14:32:56
*/
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService {

}





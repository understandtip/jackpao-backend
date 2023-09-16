package com.jackqiu.jackpao.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jackqiu.jackpao.common.ErrorCode;
import com.jackqiu.jackpao.exception.BusinessException;
import com.jackqiu.jackpao.mapper.TeamMapper;
import com.jackqiu.jackpao.model.domain.User;
import com.jackqiu.jackpao.model.domain.UserTeam;
import com.jackqiu.jackpao.model.enums.TeamStatusEnum;
import com.jackqiu.jackpao.model.request.TeamAddRequest;
import com.jackqiu.jackpao.model.request.TeamQueryRequest;
import com.jackqiu.jackpao.service.TeamService;
import com.jackqiu.jackpao.model.domain.Team;
import com.jackqiu.jackpao.service.UserService;
import com.jackqiu.jackpao.service.UserTeamService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.*;

import static cn.hutool.poi.excel.sax.AttributeName.s;
import static cn.hutool.poi.excel.sax.AttributeName.t;

/**
 * @author jackqiu
 * @description 针对表【team(队伍)】的数据库操作Service实现
 * @createDate 2023-09-07 14:32:56
 */
@Service
@Slf4j
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
        implements TeamService {

    @Autowired
    private UserTeamService userTeamService;

    @Autowired
    private UserService userService;

    /**
     * 添加队伍
     *
     * @param teamAddRequest
     * @param currentUser
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addTeam(TeamAddRequest teamAddRequest, User currentUser) {
        //1.队伍名不超过20个字符或者为空
        String name = teamAddRequest.getName();
        if (StringUtils.isBlank(name) || name.length() > 20) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "队伍名不能为空或者不能超过20个字");
        }
        //2.描述不超过 <=512（可以为空）
        String description = teamAddRequest.getDescription();
        if ( StringUtils.isNotBlank(description) && description.length() > 512) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "描述不能为空或者不能超过20个字");
        }
        //3.最大人数  [0，20]（不能为空）
        Integer maxNum = teamAddRequest.getMaxNum();
        if (maxNum == null || maxNum <= 0 || maxNum > 20) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "队伍最大人数必须大于0并且不超过20");
        }
        //4.过期时间不能早于当前时间（不能为空）
        Date expireTime = teamAddRequest.getExpireTime();
        if (null == expireTime || expireTime.before(new Date())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "时间不能为空或者过期时间不能早于当前时间");
        }
        //5.状态（默认为0，也就是公开）（可以为空）
        Integer status = teamAddRequest.getStatus();
        status = Optional.ofNullable(status).orElse(0);
        if (TeamStatusEnum.getEnumByValue(status) == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "队伍状态不满足要求");
        }
        //6.有密码时必须为加密状态 密码<=15
        String password = teamAddRequest.getPassword();
        if (TeamStatusEnum.SECRET.getCode().equals(status)) {
            if (StringUtils.isBlank(password)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "密码不能为空");
            } else if (StringUtils.isNotBlank(password) && password.length() > 15) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "密码不能大于15位");
            }
        }
        //每个用户最多只能创建5个队伍,todo VIP用户可以创建多个队伍
        Long userId = currentUser.getId();
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();
        teamQueryWrapper.eq("userId", userId);
        long count = this.count(teamQueryWrapper);
        if (count >= 5) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "最多创建5个队伍");
        }
        //保存队伍信息到team表中
        Team team = new Team();
        BeanUtil.copyProperties(teamAddRequest, team);
        team.setStatus(status);
        team.setUserId(userId);
        boolean save = this.save(team);
        if (!save) {
            log.error("更新team表失败");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新team表失败");
        }
        //保存信息到user_team表中
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(team.getId());
        userTeam.setJoinTime(new Date());
        boolean save1 = userTeamService.save(userTeam);
        if (!save1) {
            log.error("更新user_team表失败");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新user_team表失败");
        }
        return team.getId();
    }

    /**
     * 获取队伍分页列表
     *
     * @param teamQueryRequest
     * @param currentUser
     * @return
     */
    @Override
    public List<Team> getTeamList(TeamQueryRequest teamQueryRequest, User currentUser) {
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        //1.拼接查询条件,如果对应参数不为空，则拼接到查询条件中
        String name = teamQueryRequest.getName();
        if (StringUtils.isNotBlank(name)) {
            queryWrapper.like("name",name);
        }
        String description = teamQueryRequest.getDescription();
        if (StringUtils.isNotBlank(description)) {
            queryWrapper.like("description",description);
        }
        Integer maxNum = teamQueryRequest.getMaxNum();
        if (maxNum != null) {
            queryWrapper.eq("maxNum",maxNum);
        }
        Integer id = teamQueryRequest.getId();
        if (id != null && id > 0) {
            queryWrapper.eq("id",id);
        }
        Long userId = teamQueryRequest.getUserId();
        if (userId != null && userId > 0) {
            queryWrapper.eq("userId",userId);
        }
        //2.查询未过期的队伍
        queryWrapper.le("expireTime", new Date());
        //3.   状态只能为公开或者加密
        //3.1  管理员才可以查看队伍状态为私有的队伍
        Integer status = teamQueryRequest.getStatus();
        status = Optional.ofNullable(status).orElse(0);
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
        if (statusEnum != null) {
            if (statusEnum.equals(TeamStatusEnum.PRIVATE) && !userService.isAdmin(currentUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH);
            }
            queryWrapper.eq("status",status);
        }
        //4.可以通过某个关键词对队伍名和描述进行查询
        String searchText = teamQueryRequest.getSearchText();
        if (StringUtils.isNotBlank(searchText)) {
            queryWrapper.and(param -> param.like("name",searchText).or().like("description",searchText));
        }
        //分页查询
        Page<Team> userPage = new Page<>(teamQueryRequest.getPageNum(), teamQueryRequest.getPageSize());
        Page<Team> page = this.page(userPage, queryWrapper);

        return null;
    }
}





package com.jackqiu.jackpao.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jackqiu.jackpao.common.ErrorCode;
import com.jackqiu.jackpao.exception.BusinessException;
import com.jackqiu.jackpao.mapper.TeamMapper;
import com.jackqiu.jackpao.model.domain.User;
import com.jackqiu.jackpao.model.domain.UserTeam;
import com.jackqiu.jackpao.model.enums.TeamStatusEnum;
import com.jackqiu.jackpao.model.request.TeamAddRequest;
import com.jackqiu.jackpao.model.request.TeamQueryRequest;
import com.jackqiu.jackpao.model.request.TeamUpdateRequest;
import com.jackqiu.jackpao.model.vo.TeamVO;
import com.jackqiu.jackpao.service.TeamService;
import com.jackqiu.jackpao.model.domain.Team;
import com.jackqiu.jackpao.service.UserService;
import com.jackqiu.jackpao.service.UserTeamService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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

    @Autowired
    private TeamMapper teamMapper;

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
    public List<TeamVO> getTeamList(TeamQueryRequest teamQueryRequest, User currentUser) {
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
        queryWrapper.gt("expireTime", new Date());
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
        //查询符合条件的数据
        List<Team> list = this.list(queryWrapper);
        //每一个Team对象拷贝到TeamVO中
        List<TeamVO> teamVOList = list.stream().map(team -> {
            TeamVO teamVO = new TeamVO();
            BeanUtil.copyProperties(team, teamVO);
            return teamVO;
        }).collect(Collectors.toList());
        // 关联队伍的已加入队伍的用户
        return teamVOList.stream()
                .map(teamVO -> {
                    Long teamVOId = teamVO.getId();
                    teamVO.setUserList(teamMapper.associatedUsers(teamVOId).stream()
                            .map(user -> userService.getSafetyUser(user))
                            .collect(Collectors.toList()) );
                    return teamVO;
                })
                .collect(Collectors.toList());
    }

    /**
     * 更新队伍信息
     * @param teamUpdateRequest
     * @return
     */
    @Override
    public boolean updateTeam(TeamUpdateRequest teamUpdateRequest,User currentUser) {
        //1.请求数据为空，抛出异常
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        Long id = teamUpdateRequest.getId();
        //1.1  传递的id应该大于0  并且不为空
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,"id应该不为空并且大于0");
        }
        Team team = this.getById(id);
        //2.旧队伍信息不存在，抛出异常
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR,"队伍不存在");
        }
        //3.管理员或者队伍的创建者才能更新信息
        if (team.getUserId() != currentUser.getId() && !userService.isAdmin(currentUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        //如果传递的新值和原来的一样，就不更新了
        if (teamUpdateRequest.equals(team)) {
            return true;
        }
        //4.如果改为加密状态，必须传递密码
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(teamUpdateRequest.getStatus());
        TeamStatusEnum oldStatusEnum = TeamStatusEnum.getEnumByValue(team.getStatus());
        //本来是加密房间，更新为加密，可以不用传递密码
        //本来不是加密房间，更新为加密，必须要传递密码
        if (TeamStatusEnum.SECRET.equals(statusEnum) && !TeamStatusEnum.SECRET.equals(oldStatusEnum) ){
            if (StringUtils.isBlank(teamUpdateRequest.getPassword())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,"加密房间必须传递密码");
            }
        }
        Team newTeam = new Team();
        BeanUtil.copyProperties(teamUpdateRequest,newTeam);
        //如果String类型的数据为 "" 也不需要更新
        if ("".equals(teamUpdateRequest.getName())) {
            newTeam.setName(null);
        }
        if ("".equals(teamUpdateRequest.getDescription())) {
            newTeam.setDescription(null);
        }
        if ("".equals(teamUpdateRequest.getPassword())) {
            newTeam.setPassword(null);
        }
        //5.更新数据
        return this.updateById(newTeam);
    }
}





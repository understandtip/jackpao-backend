package com.jackqiu.jackpao.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jackqiu.jackpao.common.DeleteRequest;
import com.jackqiu.jackpao.common.ErrorCode;
import com.jackqiu.jackpao.exception.BusinessException;
import com.jackqiu.jackpao.mapper.TeamMapper;
import com.jackqiu.jackpao.model.domain.User;
import com.jackqiu.jackpao.model.domain.UserTeam;
import com.jackqiu.jackpao.model.enums.TeamStatusEnum;
import com.jackqiu.jackpao.model.request.*;
import com.jackqiu.jackpao.model.vo.TeamVO;
import com.jackqiu.jackpao.service.TeamService;
import com.jackqiu.jackpao.model.domain.Team;
import com.jackqiu.jackpao.service.UserService;
import com.jackqiu.jackpao.service.UserTeamService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.formula.functions.T;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.jackqiu.jackpao.common.KeyNameEnum.SYSTEM_NAME;
import static com.jackqiu.jackpao.constant.LockConstant.joinTeamLock;

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

    @Resource
    private RedissonClient redissonClient;

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
        if (StringUtils.isNotBlank(description) && description.length() > 512) {
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
            queryWrapper.like("name", name);
        }
        String description = teamQueryRequest.getDescription();
        if (StringUtils.isNotBlank(description)) {
            queryWrapper.like("description", description);
        }
        List<Long> idList = teamQueryRequest.getIdList();
        if (CollectionUtils.isNotEmpty(idList)) {
            queryWrapper.in("id",idList);
        }
        Integer maxNum = teamQueryRequest.getMaxNum();
        if (maxNum != null) {
            queryWrapper.eq("maxNum", maxNum);
        }
        Integer id = teamQueryRequest.getId();
        if (id != null && id > 0) {
            queryWrapper.eq("id", id);
        }
        Long userId = teamQueryRequest.getUserId();
        if (userId != null && userId > 0) {
            queryWrapper.eq("userId", userId);
        }
        //2.查询未过期的队伍
        queryWrapper.gt("expireTime", new Date());
        //3.   状态只能为公开或者加密
        //3.1  管理员才可以查看队伍状态为私有的队伍
        //如果用户查询我创建的队伍  或者  查询我加入的队伍时，都放行
        boolean flag = (userId != null && userId.equals(currentUser.getId())) || CollectionUtils.isNotEmpty(idList);
        if (!flag) {
            Integer status = teamQueryRequest.getStatus();
            status = Optional.ofNullable(status).orElse(0);
            TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
            if (statusEnum != null) {
                if (statusEnum.equals(TeamStatusEnum.PRIVATE) && !userService.isAdmin(currentUser)) {
                    throw new BusinessException(ErrorCode.NO_AUTH);
                }
                queryWrapper.eq("status", status);
            }
        }
        //4.可以通过某个关键词对队伍名和描述进行查询
        String searchText = teamQueryRequest.getSearchText();
        if (StringUtils.isNotBlank(searchText)) {
            queryWrapper.and(param -> param.like("name", searchText).or().like("description", searchText));
        }
        //5.关联已加入队伍的用户
        //5.1查询符合条件的List<Team>数据
        int pageSize = teamQueryRequest.getPageSize();//页面大小
        int pageNum = teamQueryRequest.getPageNum();//当前是第几页
        Page<Team> teamPage = new Page<>(pageNum, pageSize);
        List<Team> list = this.page(teamPage, queryWrapper).getRecords();
        //5.2每一个Team对象拷贝到TeamVO中
        List<TeamVO> teamVOList = list.stream().map(team -> {
            TeamVO teamVO = new TeamVO();
            BeanUtil.copyProperties(team, teamVO);
            return teamVO;
        }).collect(Collectors.toList());
        //5.3调用对应方法获取对应队伍的用户列表
        List<TeamVO> teamVOS = teamVOList.stream()
                .map(teamVO -> {
                    Long teamVOId = teamVO.getId();
                    teamVO.setUserList(teamMapper.associatedUsers(teamVOId).stream()
                            //5.4同时将用户列表脱敏
                            .map(user -> userService.getSafetyUser(user))
                            .collect(Collectors.toSet()).stream().collect(Collectors.toList()));
                    //5.5设置加入队伍的用户的数量
                    teamVO.setHasJoinNum(teamVO.getUserList().size());
                    return teamVO;
                })
                .collect(Collectors.toList());
        //5.6设置当前用户是否加入该队伍的标记
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId",currentUser.getId());
        //5.6.1 查询出用户加入的队伍id集合（使用Set集合去重）
        Set<Long> userTeamSet = userTeamService.list(userTeamQueryWrapper).stream()
                .map(userTeam -> userTeam.getTeamId()).collect(Collectors.toSet());
        //5.6.2 判断每个队伍 id 是否在集合列表中
        teamVOS.forEach(teamVO -> {
            boolean hasJoin = userTeamSet.contains(teamVO.getId());
            teamVO.setHasJoin(hasJoin);
        });
        teamVOS = teamVOS.stream().sorted(new Comparator<TeamVO>() {
            @Override
            public int compare(TeamVO o1, TeamVO o2) {
                return (o2.getHasJoinNum() * 100 / o2.getMaxNum()) - (o1.getHasJoinNum() * 100 / o1.getMaxNum());
            }
        }).collect(Collectors.toList());
        return teamVOS;
    }

    /**
     * 更新队伍信息
     *
     * @param teamUpdateRequest
     * @return
     */
    @Override
    public boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User currentUser) {
        //1.请求数据为空，抛出异常
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        Long id = teamUpdateRequest.getId();
        //1.1  传递的id应该大于0  并且不为空
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "id应该不为空并且大于0");
        }
        Team team = this.getById(id);
        //2.旧队伍信息不存在，抛出异常
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
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
        if (TeamStatusEnum.SECRET.equals(statusEnum) && !TeamStatusEnum.SECRET.equals(oldStatusEnum)) {
            if (StringUtils.isBlank(teamUpdateRequest.getPassword())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "加密房间必须传递密码");
            }
        }
        Team newTeam = new Team();
        BeanUtil.copyProperties(teamUpdateRequest, newTeam);
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

    /**
     * 加入队伍
     *
     * @param teamJoinRequest
     * @param currentUser
     * @return
     */
    @Override
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, User currentUser) {
        //队伍必须存在
        Long teamId = teamJoinRequest.getTeamId();
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "队伍不存在");
        }
        //不能加入自己的创建的队伍（因为创建队伍时就默认加入了）
        if (team.getUserId().equals(currentUser.getId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能再次加入自己的队伍");
        }
        //不能加入私有的队伍
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(team.getStatus());
        if (TeamStatusEnum.PRIVATE.equals(statusEnum)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能加入私有的队伍");
        }
        //如果是加密的队伍，必须要输入密码并且匹配
        String password = teamJoinRequest.getPassword();
        if (TeamStatusEnum.SECRET.equals(statusEnum)) {
            if (StringUtils.isBlank(password) || !password.equals(team.getPassword())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "加入加密房间需要输入密码或者密码错误");
            }
        }
        //用户只能加入未过期的队伍
        if (team.getExpireTime().before(new Date())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能加入已经过期的队伍");
        }
        //用户只能加入未满的队伍
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);
        long hasJoinTeamUserCount = userTeamService.count(userTeamQueryWrapper);
        if (hasJoinTeamUserCount >= team.getMaxNum()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能加入已满的队伍");
        }
        // 只有一个线程能获取到锁
        RLock lock = redissonClient.getLock(String.format("%s%s:%s:%s",SYSTEM_NAME, joinTeamLock, currentUser.getId(),teamId));
        try {
            // 抢到锁并执行
            while (true) {
                if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                    System.out.println("getLock: " + Thread.currentThread().getId());
                    //用户不能加入超过5个队伍
                    Long userId = currentUser.getId();
                    userTeamQueryWrapper = new QueryWrapper<>();
                    userTeamQueryWrapper.eq("userId", userId);
                    long hasJoinTeamCount = userTeamService.count(userTeamQueryWrapper);
                    if (hasJoinTeamCount >= 5) {
                        throw new BusinessException(ErrorCode.PARAM_ERROR, "用户最多只能加入和创建5个队伍");
                    }
                    //用户不能重复加入队伍（幂等性）
                    userTeamQueryWrapper = new QueryWrapper<>();
                    userTeamQueryWrapper.eq("userId", userId);
                    userTeamQueryWrapper.eq("teamId", teamId);
                    long count = userTeamService.count(userTeamQueryWrapper);
                    if (count > 0) {
                        throw new BusinessException(ErrorCode.PARAM_ERROR, "用户不能重复加入队伍");
                    }
                    //新增user_team表数据
                    UserTeam userTeam = new UserTeam();
                    userTeam.setUserId(userId);
                    userTeam.setTeamId(teamId);
                    userTeam.setJoinTime(new Date());
                    return userTeamService.save(userTeam);
                }
            }
        } catch (InterruptedException e) {
            log.error("doJoinTeamLock error", e);
            return false;
        } finally {
            // 只能释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                System.out.println("unLock: " + Thread.currentThread().getId());
                lock.unlock();
            }
        }
    }

    /**
     * 退出队伍
     *
     * @param teamQuitRequest
     * @param currentUser
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean quitTeam(TeamQuitRequest teamQuitRequest, User currentUser) {
        Long teamId = teamQuitRequest.getTeamId();
        Team team = getTeamById(teamId);
        //用户必须已经加入了队伍
        Long userId = currentUser.getId();
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId",teamId);
        userTeamQueryWrapper.eq("userId",userId);
        long userJoinTeamCount = userTeamService.count(userTeamQueryWrapper);
        if (userJoinTeamCount == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,"还未加入该队伍，无法退出");
        }
        //如果只有一个人直接退出（删除user_team表中的数据，删除队伍）
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId",teamId);
        long count = userTeamService.count(queryWrapper);
        if (count == 1 ) {
            //删除user_team表中的数据
            boolean remove = userTeamService.remove(userTeamQueryWrapper);
            if (!remove) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,"删除user_team表中的数据失败");
            }
            //删除队伍
            removeTeamById(teamId);
            return true;
        } else {//如果不只一人在队伍中
            //退出的人为队长，顺位给下一个（找出加入该队伍的人，并找出在user_team中按加入时间升序的前两位，删除队长在user_team中的关系，修改team表中的userId为下一个人）
            if (userId.equals(team.getUserId())) {
                queryWrapper.orderBy(true,true,"joinTime");
                List<UserTeam> list = userTeamService.list(queryWrapper).stream().limit(2).collect(Collectors.toList());
                boolean remove = userTeamService.remove(userTeamQueryWrapper);
                if (!remove) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR,"删除user_team表中的数据失败");
                }
                Team updateTeam = new Team();
                updateTeam.setId(teamId);
                updateTeam.setUserId(list.get(1).getUserId());
                boolean b = this.updateById(updateTeam);
                if (!b) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR,"更新team表中userId字段失败");
                }
                return true;
            } else {//如果不是队长，直接退出（删除user_team表中的数据）
                boolean remove = userTeamService.remove(userTeamQueryWrapper);
                if (!remove) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR,"删除user_team表中的数据失败");
                }
                return true;
            }
        }
    }



    /**
     * 解散队伍
     * @param deleteRequest
     * @param currentUser
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTeam(DeleteRequest deleteRequest, User currentUser) {
        long teamId = deleteRequest.getId();
        //队伍应该存在
        Team team = getTeamById(teamId);
        //当前用户必须是队伍的队长
        Long userId = currentUser.getId();
        if (!team.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN,"您不是该队伍的队长，无法解散队伍");
        }
        //删除team表中的队伍信息
        removeTeamById(teamId);
        //删除user_team表中的关联关系
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId",teamId);
        boolean remove = userTeamService.remove(userTeamQueryWrapper);
        if (!remove) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"删除user_team表中的数据失败");
        }
        return remove;
    }

    /**
     * 根据队伍id删除队伍
     * @param teamId
     */
    private void removeTeamById(long teamId) {
        boolean b = this.removeById(teamId);
        if (!b) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"删除team表中的数据失败");
        }
    }

    /**
     * 根据队伍id获取队伍信息
     * @param teamId
     * @return
     */
    private Team getTeamById(Long teamId) {
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请求参数错误");
        }
        Team team = this.getById(teamId);
        //队伍应该存在
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "队伍不存在");
        }
        return team;
    }
}





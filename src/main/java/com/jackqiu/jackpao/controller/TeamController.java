package com.jackqiu.jackpao.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jackqiu.jackpao.common.BaseResponse;
import com.jackqiu.jackpao.common.DeleteRequest;
import com.jackqiu.jackpao.common.ErrorCode;
import com.jackqiu.jackpao.common.ResultUtil;
import com.jackqiu.jackpao.exception.BusinessException;
import com.jackqiu.jackpao.model.domain.Team;
import com.jackqiu.jackpao.model.domain.User;
import com.jackqiu.jackpao.model.domain.UserTeam;
import com.jackqiu.jackpao.model.request.*;
import com.jackqiu.jackpao.model.vo.TeamVO;
import com.jackqiu.jackpao.service.TeamService;
import com.jackqiu.jackpao.service.UserService;
import com.jackqiu.jackpao.service.UserTeamService;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.apache.ibatis.annotations.Delete;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 队伍相关的控制器逻辑
 *
 * @author jackqiu
 */
@RestController
@RequestMapping("/team")
public class TeamController {

    @Autowired
    private UserService userService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private UserTeamService userTeamService;

    /**
     * 添加队伍
     *
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request) {
        //  如果请求参数为空，则抛出异常
        if (teamAddRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        // 获取当前用户
        User currentUser = userService.getCurrentUser(request);
        Long teamId = teamService.addTeam(teamAddRequest, currentUser);
        return ResultUtil.success(teamId);
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest, HttpServletRequest request) {
        //1.请求数据为空，抛出异常
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        User currentUser = userService.getCurrentUser(request);
        boolean flag = teamService.updateTeam(teamUpdateRequest, currentUser);
        //更新失败，抛出异常
        if (!flag) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败");
        }
        return ResultUtil.success(flag);
    }

    @GetMapping("/get")
    public BaseResponse<Team> getTeamById(long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        Team team = teamService.getById(id);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        return ResultUtil.success(team);
    }

    /**
     * 查询队伍列表
     *
     * @param request
     * @return
     */
    @GetMapping("/list")
    public BaseResponse<List<TeamVO>> getTeamList(TeamQueryRequest teamQueryRequest, HttpServletRequest request) {
        if (teamQueryRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        User currentUser = userService.getCurrentUser(request);
        /*if (userService.isAdmin(currentUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }*/
        List<TeamVO> list = teamService.getTeamList(teamQueryRequest, currentUser);
        return ResultUtil.success(list);
    }

    /**
     * 查询队伍列表
     *
     * @param request
     * @return
     */
    @GetMapping("/list/page")
    public BaseResponse<List<TeamVO>> getTeamListByPage(TeamQueryRequest teamQueryRequest, HttpServletRequest request) {
        BaseResponse<List<TeamVO>> teamSuccessList = getTeamList(teamQueryRequest, request);
        List<TeamVO> list = teamSuccessList.getData();
        //分页查询数据（分页出对应的数据）
        int pageSize = teamQueryRequest.getPageSize();//页面大小
        int pageNum = teamQueryRequest.getPageNum();//当前是第几页
        List<TeamVO> teamVOS = list.stream()
                .skip((pageNum - 1) * pageSize)
                .limit(pageSize).collect(Collectors.toList());
        return ResultUtil.success(teamVOS);
    }

    /**
     * 加入队伍
     *
     * @param teamJoinRequest
     * @param request
     * @return
     */
    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest, HttpServletRequest request) {
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "请求参数为空");
        }
        User currentUser = userService.getCurrentUser(request);
        boolean flag = teamService.joinTeam(teamJoinRequest, currentUser);
        return ResultUtil.success(flag);
    }

    /**
     * 退出队伍
     *
     * @param teamQuitRequest
     * @param request
     * @return
     */
    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest, HttpServletRequest request) {
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请求参数不能为空");
        }
        User currentUser = userService.getCurrentUser(request);
        boolean flag = teamService.quitTeam(teamQuitRequest, currentUser);
        if (!flag) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "退出队伍失败");
        }
        return ResultUtil.success(flag);
    }

    /**
     * 解散队伍
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请求参数不能为空");
        }
        User currentUser = userService.getCurrentUser(request);
        boolean flag = teamService.deleteTeam(deleteRequest, currentUser);
        if (!flag) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "解散队伍失败");
        }
        return ResultUtil.success(flag);
    }

    /**
     * 获取我创建的队伍
     *
     * @param teamQueryRequest
     * @param request
     * @return
     */
    @GetMapping("/list/my/create")
    public BaseResponse<List<TeamVO>> getMyCreateTeam(TeamQueryRequest teamQueryRequest, HttpServletRequest request) {
        //  a. 请求参数不为空
        if (teamQueryRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "请求参数不能为空");
        }
        //  b. 获取当前登录用户
        User currentUser = userService.getCurrentUser(request);
        //  c. 设置查询条件中的userId为当前用户
        teamQueryRequest.setUserId(currentUser.getId());
        //  d. 复用队伍列表方法
        List<TeamVO> teamList = teamService.getTeamList(teamQueryRequest, currentUser);
        return ResultUtil.success(teamList);
    }

    /**
     * 获取我加入的队伍
     * @param teamQueryRequest
     * @param request
     * @return
     */
    @GetMapping("/list/my/join")
    public BaseResponse<List<TeamVO>> getMyJoinTeam(TeamQueryRequest teamQueryRequest, HttpServletRequest request) {
        //  a. 请求参数不为空
        if (teamQueryRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "请求参数不能为空");
        }
        User currentUser = userService.getCurrentUser(request);
        //  b. 从user_team表中查询我加入了队伍的消息（将userId作为参数）
        Long userId = currentUser.getId();
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId", userId);
        List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
        //  c. 根据list集合获取队伍Id的集合idList
        List<Long> idList = userTeamList.stream().map(UserTeam::getTeamId).collect(Collectors.toList());
        //  d. 将对应的idList结果收集为set集合,再转换为List集合（防止有多条相同的数据）
        idList = idList.stream().collect(Collectors.toSet()).stream().collect(Collectors.toList());
        //  e. 复用getTeamList()方法,将idList作为参数进行传递
        teamQueryRequest.setIdList(idList);
        List<TeamVO> teamList = teamService.getTeamList(teamQueryRequest, currentUser);
        return ResultUtil.success(teamList);
    }
}

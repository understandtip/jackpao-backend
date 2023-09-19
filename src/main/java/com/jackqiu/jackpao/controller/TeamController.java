package com.jackqiu.jackpao.controller;

import com.jackqiu.jackpao.common.BaseResponse;
import com.jackqiu.jackpao.common.DeleteRequest;
import com.jackqiu.jackpao.common.ErrorCode;
import com.jackqiu.jackpao.common.ResultUtil;
import com.jackqiu.jackpao.exception.BusinessException;
import com.jackqiu.jackpao.model.domain.Team;
import com.jackqiu.jackpao.model.domain.User;
import com.jackqiu.jackpao.model.request.*;
import com.jackqiu.jackpao.model.vo.TeamVO;
import com.jackqiu.jackpao.service.TeamService;
import com.jackqiu.jackpao.service.UserService;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.apache.ibatis.annotations.Delete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

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
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,"请求参数不能为空");
        }
        User currentUser = userService.getCurrentUser(request);
        boolean flag = teamService.deleteTeam(deleteRequest, currentUser);
        if (!flag) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "解散队伍失败");
        }
        return ResultUtil.success(flag);
    }
}

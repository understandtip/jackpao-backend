package com.jackqiu.jackpao.controller;

import com.jackqiu.jackpao.common.BaseResponse;
import com.jackqiu.jackpao.common.ErrorCode;
import com.jackqiu.jackpao.common.ResultUtil;
import com.jackqiu.jackpao.exception.BusinessException;
import com.jackqiu.jackpao.model.domain.Team;
import com.jackqiu.jackpao.model.domain.User;
import com.jackqiu.jackpao.model.request.TeamAddRequest;
import com.jackqiu.jackpao.model.request.TeamQueryRequest;
import com.jackqiu.jackpao.model.request.TeamUpdateRequest;
import com.jackqiu.jackpao.model.vo.TeamVO;
import com.jackqiu.jackpao.service.TeamService;
import com.jackqiu.jackpao.service.UserService;
import com.sun.org.apache.xpath.internal.operations.Bool;
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
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest,HttpServletRequest request) {
        //1.请求数据为空，抛出异常
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        User currentUser = userService.getCurrentUser(request);
        boolean flag = teamService.updateTeam(teamUpdateRequest,currentUser);
        //更新失败，抛出异常
        if (!flag) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"更新失败");
        }
        return ResultUtil.success(flag);
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
        if (userService.isAdmin(currentUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        List<TeamVO> list = teamService.getTeamList(teamQueryRequest, currentUser);
        return ResultUtil.success(list);
    }
}

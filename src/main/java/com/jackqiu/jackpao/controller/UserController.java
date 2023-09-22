package com.jackqiu.jackpao.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ser.Serializers;
import com.jackqiu.jackpao.common.BaseResponse;
import com.jackqiu.jackpao.common.ErrorCode;
import com.jackqiu.jackpao.common.ResultUtil;
import com.jackqiu.jackpao.exception.BusinessException;
import com.jackqiu.jackpao.model.domain.User;
import com.jackqiu.jackpao.model.request.UserLoginRequest;
import com.jackqiu.jackpao.model.request.UserRegistryRequest;
import com.jackqiu.jackpao.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.jackqiu.jackpao.common.KeyNameEnum.SYSTEM_NAME;
import static com.jackqiu.jackpao.common.KeyNameEnum.USER_MODEL;
import static com.jackqiu.jackpao.constant.UserConstant.USER_LOGIN_STATE;

/**
 * @author jackqiu
 */
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 用户注册功能
     *
     * @param userRegistryRequest json数据格式
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<Long> registry(@RequestBody UserRegistryRequest userRegistryRequest) {
        if (userRegistryRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        String userAccount = userRegistryRequest.getUserAccount();
        String userPassword = userRegistryRequest.getUserPassword();
        String checkPassword = userRegistryRequest.getCheckPassword();
        String planetCode = userRegistryRequest.getPlanetCode();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "参数不能为空");
        }
        Long id = userService.registry(userAccount, userPassword, checkPassword, planetCode);
        return ResultUtil.success(id);
    }

    /**
     * 用户登录功能
     *
     * @param userLoginRequest json数据格式
     * @param request          HttpServletRequest对象
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<User> login(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "参数不能为空");
        }
        User user = userService.login(userAccount, userPassword, request);
        return ResultUtil.success(user);
    }

    /**
     * 登出
     *
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Integer> logout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        int result = userService.logout(request);
        return ResultUtil.success(result);
    }

    /**
     * 获取当前登录用户的信息
     *
     * @param request
     * @return
     */
    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        User currentUser = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NO_LOGIN);
        }
        Long userId = currentUser.getId();
        User user = userService.getById(userId);
        //todo 可以校验用户是否合法（比如之前是否有过违规操作）
        User safetyUser = userService.getSafetyUser(user);
        return ResultUtil.success(safetyUser);
    }

    // region 管理员

    /**
     * 根据查询参数信息查询用户（展示用户列表）
     *
     * @param username
     * @param request
     * @return
     */
    @GetMapping("/search")
    public BaseResponse<List<User>> searchUser(String username, HttpServletRequest request) {
        List<User> userList = userService.searchUser(username, request);
        return ResultUtil.success(userList);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestParam("id") Long id, HttpServletRequest request) {
        if (!userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "缺少管理员权限");
        }
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "id应该大于0");
        }
        boolean flag = userService.removeById(id);
        return ResultUtil.success(flag);
    }

    /**
     * 更新用户信息，只允许管理员或者自己进行修改
     *
     * @param user
     * @param request
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Integer> updateUser(@RequestBody User user, HttpServletRequest request) {
        if (user == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "请求参数不能为空或者");
        }
        User currentUser = userService.getCurrentUser(request);
        int flag = userService.updateUser(user, currentUser);
        return ResultUtil.success(flag);
    }

    //region 根据标签推荐用户

    /**
     * 根据标签搜索用户
     *
     * @param tagNameList
     * @return
     */
    @GetMapping("/search/tags")
    public BaseResponse<List<User>> searchUserByTags(@RequestParam(required = false) List<String> tagNameList, HttpServletRequest request) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "请求参数不能为空");
        }
        User currentUser = userService.getCurrentUser(request);
        List<User> userList = userService.searchUserByTags(tagNameList,currentUser);
        return ResultUtil.success(userList);
    }

    /**
     * 推荐用户
     *
     * @return
     */
    @GetMapping("/recommend")
    public BaseResponse<Page<User>> recommendUsers(long pageSize, long pageNum, HttpServletRequest request) {
        User currentUser = userService.getCurrentUser(request);
        if (pageSize <= 0 || pageNum <= 0 || pageSize > 100) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "分页请求参数应该大于0或者每页显示条数不能大于100");
        }
        //1.如果缓存中存在，直接返回
        String redisKey = SYSTEM_NAME + ":" + USER_MODEL + ":recommend:" + currentUser.getId();
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        Page<User> userPageInRedis = (Page<User>) ops.get(redisKey);
        if (userPageInRedis != null) {
            return ResultUtil.success(userPageInRedis);
        }
        //2.如果缓存中不存在，那么就查询数据库
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        Page<User> userPage = userService.page(new Page<>(pageNum, pageSize), queryWrapper);
        //排除自己
        List<User> realUserList = userPage.getRecords().stream().filter(user -> {
            if (user.getId().equals(currentUser.getId())) {
                return false;
            }
            return true;
        }).collect(Collectors.toList());
        userPage.setRecords(realUserList);
        //3.查询出来之后，将数据保存在缓存中，下次查询时就可以从缓存中获取
        try {
            ops.set(redisKey, userPage, 30, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("redis set key error", e);
        }
        return ResultUtil.success(userPage);
    }

    /**
     * 匹配最相似的用户
     *
     * @param num
     * @param request
     * @return
     */
    @GetMapping("/match")
    public BaseResponse<List<User>> matchUsers(long num, HttpServletRequest request) {
        if (num < 0 || num > 20) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        User currentUser = userService.getCurrentUser(request);
        //1.如果缓存中存在，直接返回
        String redisKey = String.format("%s:%s:match:%s", SYSTEM_NAME, USER_MODEL, currentUser.getId());
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        List<User> userPageInRedis = (List<User>) ops.get(redisKey);
        if (userPageInRedis != null) {
            return ResultUtil.success(userPageInRedis);
        }
        //2.如果缓存中不存在，那么就查询数据库
        List<User> userList = userService.matchUsers(num, currentUser);
        //3.查询出来之后，将数据保存在缓存中，下次查询时就可以从缓存中获取
        try {
            ops.set(redisKey, userList, 30, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("redis set key error", e);
        }
        return ResultUtil.success(userList);
    }
}

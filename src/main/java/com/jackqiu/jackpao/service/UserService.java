package com.jackqiu.jackpao.service;

import com.jackqiu.jackpao.model.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author jackqiu
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2023-09-07 14:32:56
 */
public interface UserService extends IService<User> {


    /**
     * 注册用户
     *
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     * @param planetCode
     * @return
     */
    Long registry(String userAccount, String userPassword, String checkPassword, String planetCode);

    /**
     * 用户登录
     * @param userAccount
     * @param userPassword
     * @return
     */
    User login(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取脱敏数据
     * @param user
     * @return
     */
    User getSafetyUser(User user);

    /**
     * 登出方法
     * @param request
     * @return
     */
    int logout(HttpServletRequest request);

    /**
     * 查询用户列表
     * @param username
     * @param request
     * @return
     */
    List<User> searchUser(String username, HttpServletRequest request);

    /**
     * 判断用户是否是管理员
     * @param request
     * @return
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 判断用户是否是管理员
     * @param user
     * @return
     */
    boolean isAdmin(User user);

    /**
     * 更新用户信息
     *
     * @param user
     * @return
     */
    int updateUser(User user, User currentUser);

    /**
     * 获取当前登录用户
     * @param request
     * @return
     */
    User getCurrentUser(HttpServletRequest request);

    /**
     * 根据标签搜索用户
     * @param tagList
     * @return
     */
    List<User> searchUserByTags(List<String> tagList, User currentUser);

    /**
     * 匹配最相似的用户
     * @param num
     * @param currentUser
     * @return
     */
    List<User> matchUsers(long num, User currentUser);
}

package com.jackqiu.jackpao.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jackqiu.jackpao.common.ErrorCode;
import com.jackqiu.jackpao.exception.BusinessException;
import com.jackqiu.jackpao.model.domain.User;
import com.jackqiu.jackpao.mapper.UserMapper;
import com.jackqiu.jackpao.service.UserService;
import com.jackqiu.jackpao.utils.AlgorithmUtils;
import jodd.bean.BeanUtilBean;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.jackqiu.jackpao.constant.UserConstant.ADMIN_ROLE;
import static com.jackqiu.jackpao.constant.UserConstant.USER_LOGIN_STATE;

/**
 * @author jackqiu
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2023-09-07 14:32:56
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final String SALT = "jackqiu";

    @Resource
    private UserMapper userMapper;

    /**
     * 注销
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     * @param planetCode
     * @return
     */
    @Override
    public Long registry(String userAccount, String userPassword, String checkPassword, String planetCode) {
        //1.校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "参数不能为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户名不能小于4位");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码不能小于8位");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "两次输入的密码必须一致");
        }
        if (planetCode.length() > 5) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "星球编号过长");
        }
        // 账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账号不能含有特殊字符");
        }
        //用户名不能重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        Long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户名不能重复");
        }
        // 星球编号不能重复
        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("planetCode", planetCode);
        count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "编号重复");
        }
        //将密码进行加密
        String secretPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        //将数据存入数据库
        User user = new User();
        user.setUsername(userAccount);
        user.setUserAccount(userAccount);
        user.setUserPassword(secretPassword);
        user.setUserStatus(0);
        user.setUserRole(0);
        user.setPlanetCode(planetCode);
        user.setTags("[\"java\"]");
        boolean save = this.save(user);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "插入数据失败");
        }
        return user.getId();
    }

    /**
     * 登录
     * @param userAccount
     * @param userPassword
     * @param request
     * @return
     */
    @Override
    public User login(String userAccount, String userPassword, HttpServletRequest request) {
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,"参数不能为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户名不能小于4位");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码不能小于8位");
        }
        // 账户不能包含特殊字符
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账号不能含有特殊字符");
        }
        //将密码进行加密
        String secretPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        //查询数据
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", secretPassword);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户名和密码不匹配");
        }
        //用户脱敏
        User saftUser = getSafetyUser(user);
        //保存至Session中
        request.getSession().setAttribute(USER_LOGIN_STATE,saftUser);
        return saftUser;
    }

    /**
     * 获取脱敏数据
     * @param user
     * @return
     */
    @Override
    public User getSafetyUser(User user) {
        user.setUserPassword(null);
        user.setIsDelete(null);
        user.setUpdateTime(null);
        return user;
    }

    /**
     * 登出操作
     * @param request
     * @return
     */
    @Override
    public int logout(HttpServletRequest request) {
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }

    /**
     * 查询用户列表
     * @param username
     * @param request
     * @return
     */
    @Override
    public List<User> searchUser(String username, HttpServletRequest request) {
        if (!isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH,"缺少管理员权限");
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            queryWrapper.like("username",username);
        }
        List<User> list = userMapper.selectList(queryWrapper);
        List<User> userList = list.stream().map(user -> getSafetyUser(user)).collect(Collectors.toList());
        return userList;
    }

    /**
     * 判断用户是否是管理员
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        User currentUser = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NO_LOGIN);
        }
        if (!currentUser.getUserRole().equals(ADMIN_ROLE)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isAdmin(User user) {
        if (!user.getUserRole().equals(ADMIN_ROLE)) {
            return false;
        }
        return true;
    }

    @Override
    public User getCurrentUser(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        User currentUser = (User)request.getSession().getAttribute(USER_LOGIN_STATE);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NO_LOGIN);
        }
        return currentUser;
    }

    /**
     * 根据标签搜索用户信息
     * @param tagList 标签列表
     * @return
     */
    @Override
    public List<User> searchUserByTags(List<String> tagList, User currentUser) {
        if (CollectionUtils.isEmpty(tagList)) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "请求参数不能为空");
        }
        //1.查询出所有的用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "username","userAccount","avatarUrl",
                "gender","phone","email","userStatus","createTime","userRole","planetCode","tags");
        queryWrapper.isNotNull("tags");
        queryWrapper.ne("tags","[]");
        List<User> userList = userMapper.selectList(queryWrapper);
        Gson gson = new Gson();
        //2.匹配符合标签的用户
        return userList.stream().filter(user -> {
            //排除自己
            if (user.getId().equals(currentUser.getId())) {
                return false;
            }
            String tagsStr = user.getTags();
            Set<String> tempTagNameSet = gson.fromJson(tagsStr, new TypeToken<Set<String>>() {
            }.getType());
            tempTagNameSet = Optional.ofNullable(tempTagNameSet).orElse(new HashSet<>());
            for (String tag : tagList) {
                if (tempTagNameSet.contains(tag)){
                    return true;
                }
            }
            return false;
        }).map(this::getSafetyUser).collect(Collectors.toList());
    }

    /**
     * 匹配最相似的用户
     * @param num
     * @param currentUser
     * @return
     */
    @Override
    public List<User> matchUsers(long num, User currentUser) {
        //1.查询数据
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.select("id","tags");
        userQueryWrapper.isNotNull("tags");
        List<User> userList = this.list(userQueryWrapper);
        //1.1 获取当前登录用户的标签集合
        List<String> targetTagList = JSONUtil.toList(currentUser.getTags(), String.class);
        //2.维护一个List<Pair<User,Long>> 集合, 最后取出top num 个数据
        List<Pair<User,Long>> pairList = new ArrayList<>();
        //循环所有用户，计算对应用户和当前用户的相似度（使用算法工具进行计算）
        for (User user : userList) {
            if (user.getId().equals(currentUser.getId())) {
                continue;
            }
            List<String> tagList = JSONUtil.toList(user.getTags(), String.class);
            int length = AlgorithmUtils.minDistance(targetTagList, tagList);
            Pair<User, Long> userLongPair = new Pair<User, Long>(user,(long) length);
            pairList.add(userLongPair);
        }
        //3. 按照相似度，从小到大进行排序
        //3.1 取出前num个用户
        List<Pair<User, Long>> sortedList = pairList.stream()
                .sorted(Comparator.comparingLong(Pair::getSecond))
                .limit(num)
                .collect(Collectors.toList());
        //4.获取结果用户列表id列表
        List<Long> idList = sortedList.stream().map(pair -> {
            return pair.getFirst().getId();
        }).collect(Collectors.toList());
        //5. 查询数据库获取其他字段
        userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.in("id",idList);
        Map<Long, List<User>> collect = this.list(userQueryWrapper).stream()
        //6. 将符合的用户数据脱敏
                .map(this::getSafetyUser)
                //按照id分组
                .collect(Collectors.groupingBy(user -> user.getId()));
        //注意：查询数据库时的顺序可能会被打乱
        List<User> finalUserList = new ArrayList<>();
        for (Long userId : idList) {
            finalUserList.add(collect.get(userId).get(0));
        }
        return finalUserList;
    }

    @Override
    public int updateUser(User user, User currentUser) {
        Long userId = user.getId();
        if (userId <= 0 || userId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,"传递的用户id不能为空");
        }
        // todo 补充校验，如果用户没有传任何要更新的值，就直接报错，不用执行 update 语句
        if (!isAdmin(currentUser) && !userId.equals(currentUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH, "权限不足");
        }
        User oldUser = userMapper.selectById(userId);
        if (oldUser == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"用户信息不存在");
        }
        return userMapper.updateById(user);
    }
}





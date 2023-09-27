package com.jackqiu.jackpao.aop;

import com.jackqiu.jackpao.common.ErrorCode;
import com.jackqiu.jackpao.exception.BusinessException;
import com.jackqiu.jackpao.model.domain.User;
import com.jackqiu.jackpao.model.enums.UserRoleEnum;
import com.jackqiu.jackpao.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.jackqiu.jackpao.annotation.AuthCheck;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 全局权限管理拦截器
 * @author jackqiu
 */
@Aspect
@Component
@Slf4j
public class AuthInterceptor {

    @Autowired
    private UserService userService;

    /**
     * 统一权限管理
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor (ProceedingJoinPoint point, AuthCheck authCheck) throws Throwable {
        //  a. 获取方法中的注解对象
        //  b. 提取mustRule属性
        Integer mustRole = authCheck.mustRole();
        //  c. 获取Request请求对象
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest httpServletRequest = ((ServletRequestAttributes) requestAttributes).getRequest();
        //  d. 获取当前登录用户
        User currentUser = userService.getCurrentUser(httpServletRequest);
        //  e. 如果mustRule属性不为空（也就是当前方法需要某一个属性），执行下列逻辑
        if (mustRole != null) {
            //    ⅰ. 如果获取不到对应的枚举类，抛出异常
            UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
            if (mustRoleEnum == null) {
                log.error("方法中设置的权限枚举类未找到");
                throw new BusinessException(ErrorCode.NO_AUTH);
            }
            UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(currentUser.getUserRole());
            //    ⅱ. 如果用户被封号了，也抛出异常
            if (UserRoleEnum.BAN.equals(userRoleEnum)) {
                throw new BusinessException(ErrorCode.NO_AUTH, "当前用户已被封号");
            }
            //    ⅲ. 如果需要的是管理员权限，但是当前用户不是管理员，也抛出异常
            if (UserRoleEnum.ADMIN.equals(mustRoleEnum)) {
                if (!UserRoleEnum.ADMIN.equals(userRoleEnum)) {
                    throw new BusinessException(ErrorCode.NO_AUTH, "需要管理员权限");
                }
            }
        }
        //  f. 否则返回执行后的结果
        return point.proceed();
    }
}

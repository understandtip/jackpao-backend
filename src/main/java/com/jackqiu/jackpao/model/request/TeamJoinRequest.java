package com.jackqiu.jackpao.model.request;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 加入队伍请求封装类
 * @author jackqiu
 */
@Data
public class TeamJoinRequest implements Serializable {

    private static final long serialVersionUID = -8912962440884721799L;

    /**
     * id
     */
    private Long teamId;

    /**
     * 密码
     */
    private String password;

}

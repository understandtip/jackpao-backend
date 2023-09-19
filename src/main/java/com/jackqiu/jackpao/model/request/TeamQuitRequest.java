package com.jackqiu.jackpao.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 退出队伍请求封装类
 *
 * @author jackqiu
 */
@Data
public class TeamQuitRequest implements Serializable {

    private static final long serialVersionUID = -8863718907562588614L;

    /**
     * id
     */
    private Long teamId;

}

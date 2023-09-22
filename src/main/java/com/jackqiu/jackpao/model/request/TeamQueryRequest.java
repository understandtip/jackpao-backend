package com.jackqiu.jackpao.model.request;

import com.jackqiu.jackpao.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 查询队伍列表请求封装类
 *
 * @author jackqiu
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TeamQueryRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1231128054195446736L;

    /**
     * id
     */
    private Integer id;

    /**
     * id
     */
    private List<Long> idList;

    /**
     * 通用查询参数，从队伍名称和描述中查询
     */
    private String searchText;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 0 - 公开 ， 2 - 加密
     */
    private Integer status;

}

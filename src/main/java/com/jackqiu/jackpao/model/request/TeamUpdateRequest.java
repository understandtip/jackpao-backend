package com.jackqiu.jackpao.model.request;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jackqiu.jackpao.model.domain.Team;
import com.jackqiu.jackpao.model.domain.User;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 队伍
 *
 * @author jackqiu
 */
@Data
public class TeamUpdateRequest implements Serializable {

    private static final long serialVersionUID = -8896307846634481372L;

    /**
     * id
     */
    private Long id;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

    /**
     * 密码
     */
    private String password;

    public Boolean equals(Team team) {
        //获取需要和TeamUpdateRequest对比的属性
        Long id = team.getId();
        String name = team.getName();
        String description = team.getDescription();
        Date expireTime = team.getExpireTime();
        Integer status = team.getStatus();
        String password = team.getPassword();
        //获取TeamUpdateRequest的所有属性
        Long thisId = this.getId();
        String thisName = this.getName();
        String thisDescription = this.getDescription();
        Date thisExpireTime = this.getExpireTime();
        Integer thisStatus = this.getStatus();
        String thisPassword = this.getPassword();
        return id.equals(thisId)
                && (thisName == null ? true : thisName.equals(name) )
                && (thisDescription == null ? true : thisDescription.equals(description) )
                && (thisExpireTime == null ? true : thisExpireTime.equals(expireTime) )
                && (thisStatus == null ? true : thisStatus.equals(status) )
                && (thisPassword == null ? true : thisPassword.equals(password) );
    }
}
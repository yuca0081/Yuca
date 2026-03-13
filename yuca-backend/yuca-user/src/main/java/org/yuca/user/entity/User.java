package org.yuca.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String email;

    private String phone;

    private String password;

    private String nickname;

    private String avatarUrl;

    /**
     * 状态: 1-正常, 0-禁用
     */
    private Integer status;

    /**
     * 逻辑删除: 1-已删除, 0-未删除
     */
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private LocalDateTime lastLoginTime;

    private String lastLoginIp;

    /**
     * 检查用户是否启用
     */
    public boolean isEnabled() {
        return status != null && status == 1;
    }

    /**
     * 检查是否已锁定（预留，可扩展）
     */
    public boolean isLocked() {
        return false;
    }

    /**
     * 更新登录信息
     */
    public void updateLoginInfo(String ip) {
        this.lastLoginTime = LocalDateTime.now();
        this.lastLoginIp = ip;
    }

    /**
     * 禁用用户
     */
    public void disable() {
        this.status = 0;
    }

    /**
     * 启用用户
     */
    public void enable() {
        this.status = 1;
    }
}

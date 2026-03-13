package org.yuca.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 刷新令牌实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_refresh_token")
public class RefreshToken {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String token;

    private LocalDateTime expiryTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 是否已撤销: 1-已撤销, 0-未撤销
     */
    private Integer revoked;

    /**
     * 检查令牌是否过期
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryTime);
    }

    /**
     * 检查令牌是否已撤销
     */
    public boolean checkRevoked() {
        return revoked != null && revoked == 1;
    }

    /**
     * 检查令牌是否有效
     */
    public boolean isValid() {
        return !isExpired() && !checkRevoked();
    }

    /**
     * 撤销令牌
     */
    public void revoke() {
        this.revoked = 1;
    }
}

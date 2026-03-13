package org.yuca.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 验证码实体（预留）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_verification_code")
public class VerificationCode {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String phone;

    private String code;

    /**
     * 验证码类型: REGISTER-注册, LOGIN-登录, RESET_PASSWORD-重置密码
     */
    private String codeType;

    private LocalDateTime expiryTime;

    /**
     * 是否已使用: 1-已使用, 0-未使用
     */
    private Integer used;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 检查验证码是否过期
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryTime);
    }

    /**
     * 检查验证码是否已使用
     */
    public boolean isUsed() {
        return used != null && used == 1;
    }

    /**
     * 检查验证码是否有效
     */
    public boolean isValid() {
        return !isExpired() && !isUsed();
    }

    /**
     * 标记为已使用
     */
    public void markAsUsed() {
        this.used = 1;
    }
}

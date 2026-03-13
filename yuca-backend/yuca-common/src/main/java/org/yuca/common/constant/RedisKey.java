package org.yuca.common.constant;

/**
 * Redis常量
 */
public class RedisKey {

    /**
     * Token白名单前缀
     */
    public static final String TOKEN_PREFIX = "user:token:";

    /**
     * Token黑名单前缀
     */
    public static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    /**
     * RefreshToken黑名单前缀
     */
    public static final String REFRESH_TOKEN_BLACKLIST_PREFIX = "refresh-token:blacklist:";

    /**
     * 用户信息缓存前缀
     */
    public static final String USER_INFO_PREFIX = "user:info:";

    /**
     * 登录失败计数前缀
     */
    public static final String LOGIN_FAIL_PREFIX = "login:fail:";

    /**
     * 登录锁定前缀
     */
    public static final String LOGIN_LOCKED_PREFIX = "login:locked:";

    /**
     * 验证码前缀
     */
    public static final String SMS_CODE_PREFIX = "sms:code:";

    /**
     * 验证码发送限制前缀
     */
    public static final String SMS_LIMIT_PREFIX = "sms:limit:";
}

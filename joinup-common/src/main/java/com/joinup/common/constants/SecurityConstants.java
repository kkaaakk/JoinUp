package com.joinup.common.constants;

/**
 * 安全认证相关的基础常量。
 */
public final class SecurityConstants {

    /** 标准认证头名称。 */
    public static final String AUTHORIZATION_HEADER = "Authorization";
    /** Bearer Token 固定前缀。 */
    public static final String TOKEN_PREFIX = "Bearer ";
    /** 当前阶段默认赋予已登录用户的基础角色。 */
    public static final String ROLE_USER = "ROLE_USER";

    private SecurityConstants() {
    }
}

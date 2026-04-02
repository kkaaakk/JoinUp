package com.joinup.common.constants;

/**
 * Redis key 约定。
 * 统一前缀的目的是避免不同模块后面各自拼 key，导致命名冲突。
 */
public final class CacheConstants {

    /** JWT 黑名单前缀，预留给退出登录或强制下线场景。 */
    public static final String JWT_BLACKLIST_PREFIX = "joinup:security:jwt:blacklist:";
    /** 用户上下文缓存前缀，预留给后续缓存用户快照或权限信息。 */
    public static final String USER_CONTEXT_PREFIX = "joinup:user:context:";

    private CacheConstants() {
    }
}

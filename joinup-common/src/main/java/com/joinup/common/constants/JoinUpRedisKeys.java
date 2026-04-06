package com.joinup.common.constants;

/**
 * JoinUp 平台级 Redis Key 约定。
 * <p>
 * 当前项目里已经存在 {@code SignupRedisKeys}、{@code WaitlistRedisKeys} 等面向具体模块的细粒度 Key 工具类，
 * 本类的定位不是替代它们，而是把“跨模块都需要认知的核心 Key”统一沉淀到 common 模块中，
 * 方便后续做接口文档、运维排查、缓存巡检、限流治理和排行榜扩展。
 * </p>
 * <p>
 * 如果未来要逐步统一 Key 命名风格，建议优先从本类开始向业务模块回收口径，
 * 而不是让每个模块各自继续散落拼接字符串。
 * </p>
 */
public final class JoinUpRedisKeys {

    /**
     * 私有构造方法。
     * <p>
     * 该类只提供静态方法，不允许被实例化。
     * </p>
     */
    private JoinUpRedisKeys() {
    }

    /**
     * 构造活动正式名额库存 Key。
     * <p>
     * 这个 Key 代表“活动当前剩余多少正式席位”，适合在高并发报名时作为最核心的热点缓存使用。
     * 当前代码里报名模块已经有更细粒度的剩余名额 Key，
     * 这里保留平台层统一命名，便于后续做缓存巡检、压测脚本和运维排障。
     * </p>
     *
     * @param activityId 活动 ID
     * @return 活动正式名额库存 Key
     */
    public static String activityStock(Long activityId) {
        return "joinup:activity:stock:" + activityId;
    }

    /**
     * 构造活动候补队列 Key。
     * <p>
     * 推荐使用 ZSet 保存候补顺序，score 通常取排队序号或时间戳，
     * 这样既能快速拿到第一位候补，也便于分页查看候补排名。
     * </p>
     *
     * @param activityId 活动 ID
     * @return 活动候补队列 Key
     */
    public static String activityWaitlist(Long activityId) {
        return "joinup:activity:waitlist:" + activityId;
    }

    /**
     * 构造报名幂等去重 Key。
     * <p>
     * 该 Key 用于拦截同一用户在同一活动上的重复点击、重复重试和重复投递。
     * 推荐值保存“当前用户在该活动下的报名状态快照”或“最近一次请求号”，
     * 并在活动结束后一并过期。
     * </p>
     *
     * @param activityId 活动 ID
     * @param userId 用户 ID
     * @return 报名幂等去重 Key
     */
    public static String signupDedup(Long activityId, Long userId) {
        return "joinup:signup:dedup:" + activityId + ":" + userId;
    }

    /**
     * 构造活动级分布式锁 Key。
     * <p>
     * 这个 Key 更适合作为“跨模块协调锁”的统一入口，例如活动整体状态迁移、
     * 热门活动榜单重算、活动级限流等场景。
     * 对于候补补位、成团结算这类更强业务语义的锁，当前项目依旧建议保留各自的细粒度专用 Key。
     * </p>
     *
     * @param activityId 活动 ID
     * @return 活动级分布式锁 Key
     */
    public static String activityLock(Long activityId) {
        return "joinup:lock:activity:" + activityId;
    }

    /**
     * 构造活动热度排行榜 Key。
     * <p>
     * 推荐使用 ZSet，member 为活动 ID，score 为综合热度分。
     * 排行榜可以由浏览量、报名量、收藏量、转发量等指标增量更新。
     * </p>
     *
     * @return 活动热度排行榜 Key
     */
    public static String activityHotRank() {
        return "joinup:rank:activity:hot";
    }

    /**
     * 构造活动标签热度排行榜 Key。
     * <p>
     * 推荐使用 ZSet，member 为标签值或标签 ID，score 为标签热度。
     * 这个榜单后续可以直接服务于首页推荐、热门分类和搜索联想。
     * </p>
     *
     * @return 活动标签热度排行榜 Key
     */
    public static String tagHotRank() {
        return "joinup:rank:tag:hot";
    }

    /**
     * 构造用户级限流 Key。
     * <p>
     * 这个 Key 适合放在报名、创建活动、举报等容易被刷的接口前面。
     * 推荐值使用计数器或令牌桶状态，并配合短 TTL 控制访问频率。
     * </p>
     *
     * @param userId 用户 ID
     * @return 用户限流 Key
     */
    public static String userRateLimit(Long userId) {
        return "joinup:rate_limit:user:" + userId;
    }
}

package com.joinup.signup.support;

/**
 * 报名模块 Redis Key 构造工具。
 * <p>
 * 报名链路会同时维护活动元数据、正式余量、候补余量、用户状态、请求结果等多类缓存键。
 * 将规则统一收口后，可以避免不同类里出现不一致的 Key 拼接方式。
 * </p>
 */
public final class SignupRedisKeys {

    /**
     * 私有构造方法。
     * <p>
     * 工具类仅提供静态方法，不允许创建实例。
     * </p>
     */
    private SignupRedisKeys() {
    }

    /**
     * 构造活动元数据 Key。
     *
     * @param activityId 活动 ID
     * @return 活动元数据在 Redis 中的 Hash Key
     */
    public static String activityMeta(Long activityId) {
        return "joinup:signup:activity:" + activityId + ":meta";
    }

    /**
     * 构造正式名额剩余数量 Key。
     *
     * @param activityId 活动 ID
     * @return 正式名额剩余值的 Redis Key
     */
    public static String formalRemain(Long activityId) {
        return "joinup:signup:activity:" + activityId + ":formal:remain";
    }

    /**
     * 构造候补剩余数量 Key。
     *
     * @param activityId 活动 ID
     * @return 候补余量的 Redis Key
     */
    public static String waitlistRemain(Long activityId) {
        return "joinup:signup:activity:" + activityId + ":waitlist:remain";
    }

    /**
     * 构造候补排队序号自增 Key。
     *
     * @param activityId 活动 ID
     * @return 候补排号序列 Key
     */
    public static String waitlistQueueSeq(Long activityId) {
        return "joinup:signup:activity:" + activityId + ":waitlist:queue-seq";
    }

    /**
     * 构造用户在某活动上的幂等状态 Key。
     *
     * @param activityId 活动 ID
     * @param userId 用户 ID
     * @return 用户状态 Key
     */
    public static String userState(Long activityId, Long userId) {
        return "joinup:signup:activity:" + activityId + ":user:" + userId + ":state";
    }

    /**
     * 构造用户在某活动上的结果 Key。
     *
     * @param activityId 活动 ID
     * @param userId 用户 ID
     * @return 用户结果 Key
     */
    public static String userResult(Long activityId, Long userId) {
        return "joinup:signup:user:" + userId + ":activity:" + activityId + ":result";
    }

    /**
     * 构造按请求号索引的结果 Key。
     *
     * @param requestId 请求唯一标识
     * @return 请求结果 Key
     */
    public static String requestResult(String requestId) {
        return "joinup:signup:request:" + requestId + ":result";
    }
}

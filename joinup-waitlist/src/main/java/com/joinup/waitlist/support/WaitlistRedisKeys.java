package com.joinup.waitlist.support;

/**
 * 候补模块 Redis Key 工具类。
 */
public final class WaitlistRedisKeys {

    private WaitlistRedisKeys() {
    }

    /**
     * 构造某个活动的候补队列 Key。
     *
     * @param activityId 活动 ID
     * @return 候补队列 ZSet Key
     */
    public static String queue(Long activityId) {
        return "joinup:waitlist:activity:" + activityId + ":queue";
    }

    /**
     * 构造某个活动的候补状态 Hash Key。
     *
     * @param activityId 活动 ID
     * @return 候补状态 Hash Key
     */
    public static String state(Long activityId) {
        return "joinup:waitlist:activity:" + activityId + ":state";
    }

    /**
     * 构造全局待超时确认索引 Key。
     *
     * @return 待超时确认 ZSet Key
     */
    public static String confirmTimeoutIndex() {
        return "joinup:waitlist:promotion:confirm-timeout";
    }

    /**
     * 构造某个活动的补位互斥锁 Key。
     *
     * @param activityId 活动 ID
     * @return Redisson 锁 Key
     */
    public static String promotionLock(Long activityId) {
        return "joinup:waitlist:activity:" + activityId + ":promotion:lock";
    }
}

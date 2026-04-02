package com.joinup.activity.constant;

/**
 * 活动模块 Kafka 常量定义。
 * <p>
 * 当前主要用于活动报名截止结算后的结果事件发送，后续如果活动模块扩展更多生命周期消息，
 * 也优先统一维护在这里。
 * </p>
 */
public final class ActivityKafkaConstants {

    /** 活动结算结果主题。 */
    public static final String ACTIVITY_SETTLEMENT_TOPIC = "joinup.activity.settlement";

    /** 活动结算结果消费组。 */
    public static final String ACTIVITY_SETTLEMENT_GROUP = "joinup-activity-settlement-group";

    /**
     * 私有构造方法。
     * <p>
     * 常量类不允许被实例化。
     * </p>
     */
    private ActivityKafkaConstants() {
    }
}
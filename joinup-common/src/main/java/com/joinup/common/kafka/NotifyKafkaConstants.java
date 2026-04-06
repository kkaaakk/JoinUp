package com.joinup.common.kafka;

/**
 * 通知链路使用的 Kafka 常量定义。
 * <p>
 * 这些主题横跨报名、候补、活动、信用、通知多个模块，放在 common 中可以避免其他模块反向依赖 `joinup-notify`。
 * </p>
 */
public final class NotifyKafkaConstants {

    /** 报名成功事件主题。 */
    public static final String SIGNUP_CREATED_TOPIC = "signup-created";
    /** 报名取消事件主题。 */
    public static final String SIGNUP_CANCELED_TOPIC = "signup-canceled";
    /** 候补补位成功事件主题。 */
    public static final String WAITLIST_PROMOTED_TOPIC = "waitlist-promoted";
    /** 活动成团事件主题。 */
    public static final String ACTIVITY_GROUP_SUCCESS_TOPIC = "activity-group-success";
    /** 活动流局事件主题。 */
    public static final String ACTIVITY_GROUP_FAILED_TOPIC = "activity-group-failed";
    /** 通知发送命令主题。 */
    public static final String NOTIFY_SEND_TOPIC = "notify-send";
    /** 信用变更事件主题。 */
    public static final String CREDIT_CHANGED_TOPIC = "credit-changed";

    /** 通知模块消费上游业务事件时使用的消费组。 */
    public static final String NOTIFY_BUSINESS_GROUP = "joinup-notify-business-group";
    /** 通知模块消费发送命令时使用的消费组。 */
    public static final String NOTIFY_SEND_GROUP = "joinup-notify-send-group";

    /**
     * 私有构造方法。
     * <p>
     * 常量类不允许被实例化。
     * </p>
     */
    private NotifyKafkaConstants() {
    }
}
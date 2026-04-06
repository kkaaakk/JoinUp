package com.joinup.signup.constant;

/**
 * 报名模块使用到的 Kafka 常量定义。
 * <p>
 * 这里统一收口 Topic 与消费组名称，避免生产者、消费者分别写死字符串，
 * 也方便后续扩展重试 Topic、死信 Topic 时保持命名规范一致。
 * </p>
 */
public final class SignupKafkaConstants {

    /**
     * 报名命令主题。
     * <p>
     * 报名申请、取消报名等需要异步持久化的命令，都会先发送到这个 Topic。
     * </p>
     */
    public static final String SIGNUP_COMMAND_TOPIC = "joinup.signup.command";

    /**
     * 报名命令消费组。
     * <p>
     * 该消费组负责消费报名命令并落库，属于报名模块的核心异步处理通道。
     * </p>
     */
    public static final String SIGNUP_COMMAND_GROUP = "joinup-signup-command-group";

    /**
     * 私有构造方法。
     * <p>
     * 常量类不允许被实例化。
     * </p>
     */
    private SignupKafkaConstants() {
    }
}

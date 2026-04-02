package com.joinup.signup.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joinup.common.exception.BusinessException;
import com.joinup.common.exception.ErrorCode;
import com.joinup.signup.constant.SignupKafkaConstants;
import com.joinup.signup.event.SignupCommandEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 报名命令 Kafka 生产者。
 * <p>
 * 该组件负责把报名申请、取消报名等命令事件发送到 Kafka，
 * 让数据库写入从同步 HTTP 请求线程中解耦出来。
 * </p>
 * <p>
 * 发送时使用 activityId 作为消息键，可以让同一活动的命令尽量落到同一分区，
 * 从而帮助消费端按活动维度保持更稳定的处理顺序。
 * </p>
 */
@Component
public class SignupCommandProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 构造报名命令生产者。
     *
     * @param kafkaTemplate Kafka 模板
     * @param objectMapper JSON 序列化工具
     */
    public SignupCommandProducer(KafkaTemplate<String, String> kafkaTemplate,
                                 ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 发送报名命令。
     * <p>
     * 这里会先把命令对象序列化为 JSON，再同步等待 Kafka 返回发送结果。
     * 之所以不做“只发不等”，是因为 Redis 在此之前已经预占了席位，
     * 如果消息根本没有进入 Kafka，就必须立刻感知失败并触发补偿。
     * </p>
     *
     * @param event 报名命令事件
     */
    public void send(SignupCommandEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(SignupKafkaConstants.SIGNUP_COMMAND_TOPIC,
                            String.valueOf(event.getActivityId()),
                            payload)
                    .get(5, TimeUnit.SECONDS);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.SIGNUP_EVENT_DISPATCH_FAILED, "Failed to serialize signup command");
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.SIGNUP_EVENT_DISPATCH_FAILED, "Failed to dispatch signup command");
        }
    }
}

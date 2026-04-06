package com.joinup.activity.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joinup.activity.constant.ActivityKafkaConstants;
import com.joinup.common.event.activity.ActivitySettlementCompletedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 活动结算事件 Kafka 生产者。
 * <p>
 * 该生产者负责把活动成团/流局结果发送到 Kafka，供通知模块、报名模块或后续运营模块消费。
 * </p>
 */
@Component
public class ActivitySettlementEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 构造活动结算事件生产者。
     *
     * @param kafkaTemplate Kafka 模板
     * @param objectMapper JSON 序列化工具
     */
    public ActivitySettlementEventProducer(KafkaTemplate<String, String> kafkaTemplate,
                                           ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 发送活动结算结果事件。
     * <p>
     * 当前实现采用同步等待发送确认的方式，便于后续在日志或监控中明确感知事件投递是否成功。
     * </p>
     *
     * @param event 活动结算结果事件
     */
    public void send(ActivitySettlementCompletedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(ActivityKafkaConstants.ACTIVITY_SETTLEMENT_TOPIC,
                            String.valueOf(event.getActivityId()),
                            payload)
                    .get(5, TimeUnit.SECONDS);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("活动结算事件序列化失败", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("活动结算事件发送失败", ex);
        }
    }
}
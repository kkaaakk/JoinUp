package com.joinup.notify.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joinup.common.event.notify.NotifySendEvent;
import com.joinup.common.exception.BusinessException;
import com.joinup.common.exception.ErrorCode;
import com.joinup.common.kafka.NotifyKafkaConstants;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 通知发送命令 Kafka 生产者。
 * <p>
 * 上游业务事件不会直接落通知表，而是先统一转换成 `NotifySendEvent` 并投递到 `notify-send` 主题，
 * 再由通知模块异步分发到站内信或未来的外部渠道。
 * </p>
 */
@Component
public class NotifySendEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 构造通知发送命令生产者。
     *
     * @param kafkaTemplate Kafka 模板
     * @param objectMapper JSON 序列化工具
     */
    public NotifySendEventProducer(KafkaTemplate<String, String> kafkaTemplate,
                                   ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 发送一条通知发送命令。
     *
     * @param event 通知发送命令事件
     */
    public void send(NotifySendEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(NotifyKafkaConstants.NOTIFY_SEND_TOPIC,
                            String.valueOf(event.getUserId()),
                            payload)
                    .get(5, TimeUnit.SECONDS);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.NOTIFY_EVENT_DISPATCH_FAILED, "通知发送命令序列化失败");
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.NOTIFY_EVENT_DISPATCH_FAILED, "通知发送命令投递失败");
        }
    }
}
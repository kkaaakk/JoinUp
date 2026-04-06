package com.joinup.notify.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joinup.common.event.notify.NotifySendEvent;
import com.joinup.common.kafka.NotifyKafkaConstants;
import com.joinup.notify.service.NotifyService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 通知发送命令消费者。
 * <p>
 * 该消费者负责消费 `notify-send` 主题中的统一通知命令，并调用通知服务执行最终分发和落库。
 * </p>
 */
@Component
public class NotifySendConsumer {

    private final ObjectMapper objectMapper;
    private final NotifyService notifyService;

    /**
     * 构造通知发送命令消费者。
     *
     * @param objectMapper JSON 反序列化工具
     * @param notifyService 通知模块应用服务
     */
    public NotifySendConsumer(ObjectMapper objectMapper,
                              NotifyService notifyService) {
        this.objectMapper = objectMapper;
        this.notifyService = notifyService;
    }

    /**
     * 消费一条通知发送命令。
     *
     * @param payload Kafka 原始消息体
     * @throws Exception 反序列化异常或处理异常
     */
    @KafkaListener(
            topics = NotifyKafkaConstants.NOTIFY_SEND_TOPIC,
            groupId = NotifyKafkaConstants.NOTIFY_SEND_GROUP,
            containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(String payload) throws Exception {
        NotifySendEvent event = objectMapper.readValue(payload, NotifySendEvent.class);
        notifyService.dispatch(event);
    }
}
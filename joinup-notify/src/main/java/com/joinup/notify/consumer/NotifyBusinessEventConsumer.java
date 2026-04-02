package com.joinup.notify.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joinup.common.event.activity.ActivityGroupFailedEvent;
import com.joinup.common.event.activity.ActivityGroupSuccessEvent;
import com.joinup.common.event.credit.CreditChangedEvent;
import com.joinup.common.event.notify.NotifySendEvent;
import com.joinup.common.event.signup.SignupCanceledEvent;
import com.joinup.common.event.signup.SignupCreatedEvent;
import com.joinup.common.event.waitlist.WaitlistPromotedEvent;
import com.joinup.common.kafka.NotifyKafkaConstants;
import com.joinup.notify.domain.NotifyMessageFactory;
import com.joinup.notify.producer.NotifySendEventProducer;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 上游业务通知事件消费者。
 * <p>
 * 该消费者负责监听报名、候补、成团、流局、信用变更等业务主题，
 * 然后把它们统一转换成 `NotifySendEvent` 再投递到 `notify-send`。
 * </p>
 */
@Component
public class NotifyBusinessEventConsumer {

    private final ObjectMapper objectMapper;
    private final NotifyMessageFactory notifyMessageFactory;
    private final NotifySendEventProducer notifySendEventProducer;

    /**
     * 构造业务通知事件消费者。
     *
     * @param objectMapper JSON 反序列化工具
     * @param notifyMessageFactory 通知消息工厂
     * @param notifySendEventProducer 通知发送命令生产者
     */
    public NotifyBusinessEventConsumer(ObjectMapper objectMapper,
                                       NotifyMessageFactory notifyMessageFactory,
                                       NotifySendEventProducer notifySendEventProducer) {
        this.objectMapper = objectMapper;
        this.notifyMessageFactory = notifyMessageFactory;
        this.notifySendEventProducer = notifySendEventProducer;
    }

    /**
     * 消费报名成功事件并转发成通知发送命令。
     *
     * @param payload Kafka 原始消息体
     * @throws Exception 反序列化异常或发送异常
     */
    @KafkaListener(
            topics = NotifyKafkaConstants.SIGNUP_CREATED_TOPIC,
            groupId = NotifyKafkaConstants.NOTIFY_BUSINESS_GROUP,
            containerFactory = "kafkaListenerContainerFactory")
    public void onSignupCreated(String payload) throws Exception {
        SignupCreatedEvent event = objectMapper.readValue(payload, SignupCreatedEvent.class);
        sendOne(notifyMessageFactory.buildFromSignupCreated(event));
    }

    /**
     * 消费报名取消事件并转发成通知发送命令。
     *
     * @param payload Kafka 原始消息体
     * @throws Exception 反序列化异常或发送异常
     */
    @KafkaListener(
            topics = NotifyKafkaConstants.SIGNUP_CANCELED_TOPIC,
            groupId = NotifyKafkaConstants.NOTIFY_BUSINESS_GROUP,
            containerFactory = "kafkaListenerContainerFactory")
    public void onSignupCanceled(String payload) throws Exception {
        SignupCanceledEvent event = objectMapper.readValue(payload, SignupCanceledEvent.class);
        sendOne(notifyMessageFactory.buildFromSignupCanceled(event));
    }

    /**
     * 消费候补补位成功事件并转发成通知发送命令。
     *
     * @param payload Kafka 原始消息体
     * @throws Exception 反序列化异常或发送异常
     */
    @KafkaListener(
            topics = NotifyKafkaConstants.WAITLIST_PROMOTED_TOPIC,
            groupId = NotifyKafkaConstants.NOTIFY_BUSINESS_GROUP,
            containerFactory = "kafkaListenerContainerFactory")
    public void onWaitlistPromoted(String payload) throws Exception {
        WaitlistPromotedEvent event = objectMapper.readValue(payload, WaitlistPromotedEvent.class);
        sendOne(notifyMessageFactory.buildFromWaitlistPromoted(event));
    }

    /**
     * 消费活动成团事件并转发成通知发送命令。
     *
     * @param payload Kafka 原始消息体
     * @throws Exception 反序列化异常或发送异常
     */
    @KafkaListener(
            topics = NotifyKafkaConstants.ACTIVITY_GROUP_SUCCESS_TOPIC,
            groupId = NotifyKafkaConstants.NOTIFY_BUSINESS_GROUP,
            containerFactory = "kafkaListenerContainerFactory")
    public void onActivityGroupSuccess(String payload) throws Exception {
        ActivityGroupSuccessEvent event = objectMapper.readValue(payload, ActivityGroupSuccessEvent.class);
        sendBatch(notifyMessageFactory.buildFromActivityGroupSuccess(event));
    }

    /**
     * 消费活动流局事件并转发成通知发送命令。
     *
     * @param payload Kafka 原始消息体
     * @throws Exception 反序列化异常或发送异常
     */
    @KafkaListener(
            topics = NotifyKafkaConstants.ACTIVITY_GROUP_FAILED_TOPIC,
            groupId = NotifyKafkaConstants.NOTIFY_BUSINESS_GROUP,
            containerFactory = "kafkaListenerContainerFactory")
    public void onActivityGroupFailed(String payload) throws Exception {
        ActivityGroupFailedEvent event = objectMapper.readValue(payload, ActivityGroupFailedEvent.class);
        sendBatch(notifyMessageFactory.buildFromActivityGroupFailed(event));
    }

    /**
     * 消费信用变更事件并转发成通知发送命令。
     *
     * @param payload Kafka 原始消息体
     * @throws Exception 反序列化异常或发送异常
     */
    @KafkaListener(
            topics = NotifyKafkaConstants.CREDIT_CHANGED_TOPIC,
            groupId = NotifyKafkaConstants.NOTIFY_BUSINESS_GROUP,
            containerFactory = "kafkaListenerContainerFactory")
    public void onCreditChanged(String payload) throws Exception {
        CreditChangedEvent event = objectMapper.readValue(payload, CreditChangedEvent.class);
        sendOne(notifyMessageFactory.buildFromCreditChanged(event));
    }

    /**
     * 发送一条通知发送命令。
     *
     * @param event 通知发送命令
     */
    private void sendOne(NotifySendEvent event) {
        if (event == null) {
            return;
        }
        notifySendEventProducer.send(event);
    }

    /**
     * 批量发送通知发送命令。
     *
     * @param events 通知发送命令列表
     */
    private void sendBatch(List<NotifySendEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        events.forEach(notifySendEventProducer::send);
    }
}
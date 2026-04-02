package com.joinup.activity.domain;

import com.joinup.activity.producer.ActivitySettlementEventProducer;
import com.joinup.common.event.activity.ActivitySettlementCompletedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 活动结算领域事件监听器。
 * <p>
 * 该监听器在数据库事务提交完成后，把活动结算事件继续发送到 Kafka，
 * 避免出现数据库未提交但消息先发出的不一致情况。
 * </p>
 */
@Component
public class ActivitySettlementEventListener {

    private final ActivitySettlementEventProducer activitySettlementEventProducer;

    /**
     * 构造活动结算领域事件监听器。
     *
     * @param activitySettlementEventProducer 活动结算事件 Kafka 生产者
     */
    public ActivitySettlementEventListener(ActivitySettlementEventProducer activitySettlementEventProducer) {
        this.activitySettlementEventProducer = activitySettlementEventProducer;
    }

    /**
     * 在事务提交后发送活动结算结果事件。
     *
     * @param event 活动结算结果事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onActivitySettlementCompleted(ActivitySettlementCompletedEvent event) {
        activitySettlementEventProducer.send(event);
    }
}
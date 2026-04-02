package com.joinup.common.event.activity;

import com.joinup.common.event.DomainEvent;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 活动报名截止结算完成事件。
 * <p>
 * 当活动在报名截止时间后被系统判定为“成团成功”或“成团失败”时，会发布该事件。
 * 该事件既可以作为单体内的领域事件，也可以直接作为 Kafka 的消息载荷向下游传播。
 * </p>
 */
@Getter
@Builder
public class ActivitySettlementCompletedEvent implements DomainEvent {

    private static final String EVENT_TYPE = "ACTIVITY_SETTLEMENT_COMPLETED";

    private final String eventId;
    private final Long activityId;
    private final Long organizerId;
    private final Integer fromStatus;
    private final Integer toStatus;
    private final Integer currentParticipants;
    private final Integer minGroupSize;
    private final Boolean rosterLocked;
    private final Boolean closeSignups;
    private final Boolean closeWaitlists;
    private final String reason;
    private final LocalDateTime occurredAt;

    /**
     * 返回事件唯一标识。
     *
     * @return 事件 ID
     */
    @Override
    public String eventId() {
        return eventId;
    }

    /**
     * 返回事件类型。
     *
     * @return 固定事件类型编码
     */
    @Override
    public String eventType() {
        return EVENT_TYPE;
    }

    /**
     * 返回事件发生时间。
     *
     * @return 事件发生时间
     */
    @Override
    public LocalDateTime occurredAt() {
        return occurredAt;
    }
}
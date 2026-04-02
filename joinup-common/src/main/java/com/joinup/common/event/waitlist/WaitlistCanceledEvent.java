package com.joinup.common.event.waitlist;

import com.joinup.common.event.DomainEvent;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 候补取消事件。
 * <p>
 * 该事件用于把数据库中的候补取消状态同步到 Redis 队列，避免 Redis 队列残留失效成员。
 * </p>
 */
@Getter
@Builder
public class WaitlistCanceledEvent implements DomainEvent {

    private static final String EVENT_TYPE = "WAITLIST_CANCELED";

    private final String eventId;
    private final Long activityId;
    private final Long userId;
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

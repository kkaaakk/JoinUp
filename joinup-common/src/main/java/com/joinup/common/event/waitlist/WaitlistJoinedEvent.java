package com.joinup.common.event.waitlist;

import com.joinup.common.event.DomainEvent;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 候补入队事件。
 * <p>
 * 该事件在候补记录成功写入数据库后发布，用于通知候补模块同步维护 Redis 队列缓存。
 * </p>
 */
@Getter
@Builder
public class WaitlistJoinedEvent implements DomainEvent {

    private static final String EVENT_TYPE = "WAITLIST_JOINED";

    private final String eventId;
    private final Long activityId;
    private final Long userId;
    private final Integer queueNo;
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

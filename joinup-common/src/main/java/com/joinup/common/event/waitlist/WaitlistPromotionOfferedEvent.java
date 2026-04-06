package com.joinup.common.event.waitlist;

import com.joinup.common.event.DomainEvent;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 候补补位发放事件。
 * <p>
 * 该事件表示候补模块已经为某个候补用户发放了补位机会，
 * 报名模块需要据此预留正式席位并生成正式报名记录。
 * </p>
 */
@Getter
@Builder
public class WaitlistPromotionOfferedEvent implements DomainEvent {

    private static final String EVENT_TYPE = "WAITLIST_PROMOTION_OFFERED";

    private final String eventId;
    private final Long activityId;
    private final Long userId;
    private final Integer queueNo;
    private final Integer promotionSource;
    private final LocalDateTime confirmDeadline;
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

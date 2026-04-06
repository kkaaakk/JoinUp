package com.joinup.common.event.waitlist;

import com.joinup.common.event.DomainEvent;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 候补补位超时失效事件。
 * <p>
 * 该事件表示某个已经获得补位资格的候补用户未在截止时间前确认，
 * 报名模块需要释放其占用的正式席位，并再次触发下一轮候补推进。
 * </p>
 */
@Getter
@Builder
public class WaitlistPromotionExpiredEvent implements DomainEvent {

    private static final String EVENT_TYPE = "WAITLIST_PROMOTION_EXPIRED";

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

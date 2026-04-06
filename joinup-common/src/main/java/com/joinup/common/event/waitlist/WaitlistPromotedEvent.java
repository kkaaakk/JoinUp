package com.joinup.common.event.waitlist;

import com.joinup.common.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 候补补位成功事件。
 * <p>
 * 该事件用于通知模块生成“候补成功通知”，提醒用户已经拿到正式名额。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaitlistPromotedEvent implements DomainEvent {

    private static final String EVENT_TYPE = "WAITLIST_PROMOTED";

    private String eventId;
    private Long userId;
    private Long activityId;
    private Long signupId;
    private String activityTitle;
    private LocalDateTime confirmDeadline;
    private LocalDateTime occurredAt;

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
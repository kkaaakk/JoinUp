package com.joinup.common.event.activity;

import com.joinup.common.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动流局事件。
 * <p>
 * 该事件用于通知模块向活动相关用户发送“活动流局通知”。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityGroupFailedEvent implements DomainEvent {

    private static final String EVENT_TYPE = "ACTIVITY_GROUP_FAILED";

    private String eventId;
    private Long activityId;
    private String activityTitle;
    private String reason;
    private List<Long> recipientUserIds;
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
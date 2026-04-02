package com.joinup.common.event.activity;

import com.joinup.common.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动成团成功事件。
 * <p>
 * 该事件面向通知模块，要求把成团结果通知给一组目标用户。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityGroupSuccessEvent implements DomainEvent {

    private static final String EVENT_TYPE = "ACTIVITY_GROUP_SUCCESS";

    private String eventId;
    private Long activityId;
    private String activityTitle;
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
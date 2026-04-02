package com.joinup.common.event.signup;

import com.joinup.common.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 报名成功事件。
 * <p>
 * 该事件用于在用户报名成功后驱动通知模块发送“报名成功通知”。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupCreatedEvent implements DomainEvent {

    private static final String EVENT_TYPE = "SIGNUP_CREATED";

    private String eventId;
    private Long userId;
    private Long activityId;
    private Long signupId;
    private String activityTitle;
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
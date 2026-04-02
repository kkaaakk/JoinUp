package com.joinup.common.event.signup;

import com.joinup.common.event.DomainEvent;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 正式报名释放席位事件。
 * <p>
 * 该事件表示某个正式席位已经被释放，候补模块可以尝试为该活动推进下一位候补用户。
 * 触发来源既可能是用户主动取消正式报名，也可能是候补补位超时后系统回收席位。
 * </p>
 */
@Getter
@Builder
public class FormalSignupCanceledEvent implements DomainEvent {

    private static final String EVENT_TYPE = "FORMAL_SIGNUP_CANCELED";

    private final String eventId;
    private final Long activityId;
    private final Long canceledUserId;
    private final String cancelReason;
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

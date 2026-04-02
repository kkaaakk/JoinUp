package com.joinup.common.event.signup;

import com.joinup.common.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 报名取消事件。
 * <p>
 * 该事件既可以表达用户主动取消报名，也可以表达活动取消后系统侧批量取消报名。
 * 通知模块会根据 `activityCanceled` 字段决定是否发送“活动取消通知”。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupCanceledEvent implements DomainEvent {

    private static final String EVENT_TYPE = "SIGNUP_CANCELED";

    private String eventId;
    private Long userId;
    private Long activityId;
    private Long signupId;
    private String activityTitle;
    private String reason;
    private Boolean activityCanceled;
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
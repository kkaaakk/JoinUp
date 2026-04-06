package com.joinup.common.event.credit;

import com.joinup.common.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 信用变更事件。
 * <p>
 * 该事件用于在用户信用分变化后，驱动通知模块发送“信用变更通知”。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditChangedEvent implements DomainEvent {

    private static final String EVENT_TYPE = "CREDIT_CHANGED";

    private String eventId;
    private Long userId;
    private Integer deltaScore;
    private Integer beforeScore;
    private Integer afterScore;
    private String reason;
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
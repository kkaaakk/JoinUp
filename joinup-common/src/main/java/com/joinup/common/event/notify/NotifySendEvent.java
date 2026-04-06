package com.joinup.common.event.notify;

import com.joinup.common.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知发送命令事件。
 * <p>
 * 该事件是通知模块内部真正执行发送时使用的统一命令载荷。
 * 上游业务事件会先被转换成该命令，再进入 `notify-send` 主题。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotifySendEvent implements DomainEvent {

    private static final String EVENT_TYPE = "NOTIFY_SEND";

    private String eventId;
    private Long userId;
    private Integer messageType;
    private String title;
    private String content;
    private String bizType;
    private Long bizId;
    private List<Integer> channels;
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
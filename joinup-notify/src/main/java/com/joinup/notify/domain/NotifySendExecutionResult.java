package com.joinup.notify.domain;

import com.joinup.notify.enums.NotifyStatusEnum;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 单个通知渠道的发送执行结果。
 */
@Getter
@Builder
public class NotifySendExecutionResult {

    private NotifyStatusEnum status;
    private LocalDateTime sendTime;
    private String failReason;
}
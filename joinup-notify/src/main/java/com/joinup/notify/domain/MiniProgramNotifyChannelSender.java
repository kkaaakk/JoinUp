package com.joinup.notify.domain;

import com.joinup.common.event.notify.NotifySendEvent;
import com.joinup.notify.enums.NotifyChannelEnum;
import com.joinup.notify.enums.NotifyStatusEnum;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 小程序通知发送器占位实现。
 */
@Component
public class MiniProgramNotifyChannelSender implements NotifyChannelSender {

    /**
     * 判断是否支持小程序通知渠道。
     *
     * @param channel 通知渠道
     * @return 如果是小程序通知渠道则返回 {@code true}
     */
    @Override
    public boolean supports(NotifyChannelEnum channel) {
        return channel == NotifyChannelEnum.MINI_PROGRAM;
    }

    /**
     * 执行小程序通知发送。
     *
     * @param event 通知发送命令事件
     * @param channel 本次实际发送的渠道
     * @return 当前阶段的占位发送结果
     */
    @Override
    public NotifySendExecutionResult send(NotifySendEvent event, NotifyChannelEnum channel) {
        return NotifySendExecutionResult.builder()
                .status(NotifyStatusEnum.FAILED)
                .sendTime(LocalDateTime.now())
                .failReason("小程序通知通道尚未接入具体服务")
                .build();
    }
}
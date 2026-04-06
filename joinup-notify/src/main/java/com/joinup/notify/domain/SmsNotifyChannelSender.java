package com.joinup.notify.domain;

import com.joinup.common.event.notify.NotifySendEvent;
import com.joinup.notify.enums.NotifyChannelEnum;
import com.joinup.notify.enums.NotifyStatusEnum;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 短信通知发送器占位实现。
 * <p>
 * 当前项目阶段还未接入短信供应商，因此这里先保留标准接口和失败占位结果。
 * </p>
 */
@Component
public class SmsNotifyChannelSender implements NotifyChannelSender {

    /**
     * 判断是否支持短信渠道。
     *
     * @param channel 通知渠道
     * @return 如果是短信渠道则返回 {@code true}
     */
    @Override
    public boolean supports(NotifyChannelEnum channel) {
        return channel == NotifyChannelEnum.SMS;
    }

    /**
     * 执行短信发送。
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
                .failReason("短信通道尚未接入具体供应商")
                .build();
    }
}
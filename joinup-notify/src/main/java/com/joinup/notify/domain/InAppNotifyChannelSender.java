package com.joinup.notify.domain;

import com.joinup.common.event.notify.NotifySendEvent;
import com.joinup.notify.enums.NotifyChannelEnum;
import com.joinup.notify.enums.NotifyStatusEnum;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 站内信发送器。
 * <p>
 * 当前站内信不需要调用外部供应商，因此这里只需要返回“发送成功”，真正的落库由应用服务统一处理。
 * </p>
 */
@Component
public class InAppNotifyChannelSender implements NotifyChannelSender {

    /**
     * 判断是否支持站内信渠道。
     *
     * @param channel 通知渠道
     * @return 如果是站内信渠道则返回 {@code true}
     */
    @Override
    public boolean supports(NotifyChannelEnum channel) {
        return channel == NotifyChannelEnum.IN_APP;
    }

    /**
     * 执行站内信发送。
     *
     * @param event 通知发送命令事件
     * @param channel 本次实际发送的渠道
     * @return 站内信发送结果
     */
    @Override
    public NotifySendExecutionResult send(NotifySendEvent event, NotifyChannelEnum channel) {
        return NotifySendExecutionResult.builder()
                .status(NotifyStatusEnum.SENT)
                .sendTime(LocalDateTime.now())
                .build();
    }
}
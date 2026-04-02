package com.joinup.notify.domain;

import com.joinup.common.event.notify.NotifySendEvent;
import com.joinup.notify.enums.NotifyChannelEnum;

/**
 * 通知渠道发送器。
 * <p>
 * 不同渠道的发送细节完全不同，因此通过该接口把“渠道适配层”从应用服务中拆出来。
 * </p>
 */
public interface NotifyChannelSender {

    /**
     * 判断当前发送器是否支持指定渠道。
     *
     * @param channel 通知渠道
     * @return 如果支持该渠道则返回 {@code true}
     */
    boolean supports(NotifyChannelEnum channel);

    /**
     * 执行一次通知发送。
     *
     * @param event 通知发送命令事件
     * @param channel 本次实际发送的渠道
     * @return 发送执行结果
     */
    NotifySendExecutionResult send(NotifySendEvent event, NotifyChannelEnum channel);
}
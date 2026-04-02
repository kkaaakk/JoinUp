package com.joinup.notify.service;

import com.joinup.common.event.notify.NotifySendEvent;
import com.joinup.notify.dto.NotifyPageQuery;
import com.joinup.notify.vo.NotifyPageVO;

/**
 * 通知模块应用服务。
 */
public interface NotifyService {

    /**
     * 分页查询当前用户的站内通知。
     *
     * @param currentUserId 当前用户 ID
     * @param query 分页查询条件
     * @return 分页结果
     */
    NotifyPageVO pageCurrentUserNotifications(Long currentUserId, NotifyPageQuery query);

    /**
     * 把一条通知标记为已读。
     *
     * @param currentUserId 当前用户 ID
     * @param messageId 通知 ID
     */
    void markRead(Long currentUserId, Long messageId);

    /**
     * 执行一次通知发送命令。
     *
     * @param event 通知发送命令事件
     */
    void dispatch(NotifySendEvent event);
}
package com.joinup.notify.domain;

import com.joinup.common.event.activity.ActivityGroupFailedEvent;
import com.joinup.common.event.activity.ActivityGroupSuccessEvent;
import com.joinup.common.event.credit.CreditChangedEvent;
import com.joinup.common.event.notify.NotifySendEvent;
import com.joinup.common.event.signup.SignupCanceledEvent;
import com.joinup.common.event.signup.SignupCreatedEvent;
import com.joinup.common.event.waitlist.WaitlistPromotedEvent;
import com.joinup.notify.enums.NotifyChannelEnum;
import com.joinup.notify.enums.NotifyMessageTypeEnum;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 通知消息工厂。
 * <p>
 * 该工厂负责把上游业务事件翻译成统一的 `NotifySendEvent`，从而把文案组装和通知发送流程解耦。
 * </p>
 */
@Component
public class NotifyMessageFactory {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String BIZ_TYPE_SIGNUP = "SIGNUP";
    private static final String BIZ_TYPE_ACTIVITY = "ACTIVITY";
    private static final String BIZ_TYPE_CREDIT = "CREDIT";

    /**
     * 根据报名成功事件生成通知发送命令。
     *
     * @param event 报名成功事件
     * @return 通知发送命令
     */
    public NotifySendEvent buildFromSignupCreated(SignupCreatedEvent event) {
        return buildSingleUserEvent(event.getUserId(),
                NotifyMessageTypeEnum.SIGNUP_SUCCESS,
                "报名成功",
                "你已成功报名活动「" + event.getActivityTitle() + "」，请按时参加。",
                BIZ_TYPE_SIGNUP,
                event.getSignupId());
    }

    /**
     * 根据报名取消事件生成通知发送命令。
     *
     * @param event 报名取消事件
     * @return 通知发送命令
     */
    public NotifySendEvent buildFromSignupCanceled(SignupCanceledEvent event) {
        if (Boolean.TRUE.equals(event.getActivityCanceled())) {
            return buildSingleUserEvent(event.getUserId(),
                    NotifyMessageTypeEnum.ACTIVITY_CANCELED,
                    "活动已取消",
                    "活动「" + event.getActivityTitle() + "」已取消，你的报名已失效。",
                    BIZ_TYPE_ACTIVITY,
                    event.getActivityId());
        }
        return buildSingleUserEvent(event.getUserId(),
                NotifyMessageTypeEnum.SYSTEM_NOTICE,
                "报名状态变更",
                "你在活动「" + event.getActivityTitle() + "」中的报名已取消。",
                BIZ_TYPE_SIGNUP,
                event.getSignupId());
    }

    /**
     * 根据候补补位成功事件生成通知发送命令。
     *
     * @param event 候补补位成功事件
     * @return 通知发送命令
     */
    public NotifySendEvent buildFromWaitlistPromoted(WaitlistPromotedEvent event) {
        String content = "你已从活动「" + event.getActivityTitle() + "」的候补队列转为正式名额";
        if (event.getConfirmDeadline() != null) {
            content = content + "，请在 " + DATE_TIME_FORMATTER.format(event.getConfirmDeadline()) + " 前完成确认。";
        } else {
            content = content + "。";
        }
        return buildSingleUserEvent(event.getUserId(),
                NotifyMessageTypeEnum.WAITLIST_PROMOTED,
                "候补成功",
                content,
                BIZ_TYPE_SIGNUP,
                event.getSignupId());
    }

    /**
     * 根据活动成团事件生成通知发送命令列表。
     *
     * @param event 活动成团事件
     * @return 通知发送命令列表
     */
    public List<NotifySendEvent> buildFromActivityGroupSuccess(ActivityGroupSuccessEvent event) {
        if (event.getRecipientUserIds() == null || event.getRecipientUserIds().isEmpty()) {
            return Collections.emptyList();
        }
        return event.getRecipientUserIds().stream()
                .map(userId -> buildSingleUserEvent(userId,
                        NotifyMessageTypeEnum.ACTIVITY_GROUP_SUCCESS,
                        "活动已成团",
                        "活动「" + event.getActivityTitle() + "」已成团，请留意后续开始时间。",
                        BIZ_TYPE_ACTIVITY,
                        event.getActivityId()))
                .toList();
    }

    /**
     * 根据活动流局事件生成通知发送命令列表。
     *
     * @param event 活动流局事件
     * @return 通知发送命令列表
     */
    public List<NotifySendEvent> buildFromActivityGroupFailed(ActivityGroupFailedEvent event) {
        if (event.getRecipientUserIds() == null || event.getRecipientUserIds().isEmpty()) {
            return Collections.emptyList();
        }
        String content = "活动「" + event.getActivityTitle() + "」未达到成团条件，已流局。";
        if (event.getReason() != null && !event.getReason().isBlank()) {
            content = content + " 原因：" + event.getReason();
        }
        String finalContent = content;
        return event.getRecipientUserIds().stream()
                .map(userId -> buildSingleUserEvent(userId,
                        NotifyMessageTypeEnum.ACTIVITY_GROUP_FAILED,
                        "活动已流局",
                        finalContent,
                        BIZ_TYPE_ACTIVITY,
                        event.getActivityId()))
                .toList();
    }

    /**
     * 根据信用变更事件生成通知发送命令。
     *
     * @param event 信用变更事件
     * @return 通知发送命令
     */
    public NotifySendEvent buildFromCreditChanged(CreditChangedEvent event) {
        String direction = event.getDeltaScore() != null && event.getDeltaScore() >= 0 ? "+" : "";
        return buildSingleUserEvent(event.getUserId(),
                NotifyMessageTypeEnum.CREDIT_CHANGED,
                "信用分已变更",
                "你的信用分发生变更：" + direction + event.getDeltaScore()
                        + "，当前信用分为 " + event.getAfterScore() + "。原因：" + event.getReason(),
                BIZ_TYPE_CREDIT,
                event.getUserId());
    }

    /**
     * 构造一条面向单个用户的通知发送命令。
     *
     * @param userId 接收用户 ID
     * @param messageType 消息类型
     * @param title 通知标题
     * @param content 通知内容
     * @param bizType 业务类型
     * @param bizId 业务主键
     * @return 通知发送命令
     */
    private NotifySendEvent buildSingleUserEvent(Long userId,
                                                 NotifyMessageTypeEnum messageType,
                                                 String title,
                                                 String content,
                                                 String bizType,
                                                 Long bizId) {
        return NotifySendEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .userId(userId)
                .messageType(messageType.getCode())
                .title(title)
                .content(content)
                .bizType(bizType)
                .bizId(bizId)
                .channels(List.of(NotifyChannelEnum.IN_APP.getCode()))
                .occurredAt(java.time.LocalDateTime.now())
                .build();
    }
}
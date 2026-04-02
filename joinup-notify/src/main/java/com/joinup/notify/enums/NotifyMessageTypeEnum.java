package com.joinup.notify.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 通知消息业务类型枚举。
 */
@Getter
@RequiredArgsConstructor
public enum NotifyMessageTypeEnum {
    ACTIVITY_REMINDER(10, "活动提醒"),
    SIGNUP_RESULT(20, "报名结果"),
    WAITLIST_PROMOTION(30, "候补补位"),
    CREDIT_CHANGE(40, "信用变更"),
    SYSTEM_NOTICE(50, "系统通知");

    private final int code;
    private final String description;
}

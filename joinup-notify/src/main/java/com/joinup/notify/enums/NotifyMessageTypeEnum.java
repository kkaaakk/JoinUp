package com.joinup.notify.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 通知消息类型枚举。
 * <p>
 * 这里按具体业务场景建模，而不是按模块建模，目的是让页面展示和通知文案生成都能直接复用该类型。
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum NotifyMessageTypeEnum {
    SIGNUP_SUCCESS(10, "报名成功通知"),
    WAITLIST_PROMOTED(20, "候补成功通知"),
    ACTIVITY_GROUP_SUCCESS(30, "成团通知"),
    ACTIVITY_GROUP_FAILED(40, "流局通知"),
    ACTIVITY_START_REMINDER(50, "即将开始提醒"),
    ACTIVITY_CANCELED(60, "活动取消通知"),
    CREDIT_CHANGED(70, "信用变更通知"),
    SYSTEM_NOTICE(80, "系统通知");

    private final int code;
    private final String description;

    /**
     * 根据消息类型编码反查枚举。
     *
     * @param code 消息类型编码
     * @return 匹配到的枚举；如果没有匹配项则返回 {@code null}
     */
    public static NotifyMessageTypeEnum fromCode(Integer code) {
        for (NotifyMessageTypeEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }
}
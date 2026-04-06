package com.joinup.activity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 活动状态枚举。
 */
@Getter
@RequiredArgsConstructor
public enum ActivityStatusEnum {
    DRAFT(10, "草稿"),
    PENDING_REVIEW(20, "待审核"),
    SIGNUP_OPEN(30, "报名中"),
    FULL(40, "已满员"),
    WAITLIST_OPEN(50, "候补开放"),
    GROUP_SUCCESS(60, "成团成功"),
    GROUP_FAILED(70, "成团失败"),
    IN_PROGRESS(80, "进行中"),
    FINISHED(90, "已结束"),
    CANCELED(100, "已取消");

    private final int code;
    private final String description;

    public static ActivityStatusEnum fromCode(Integer code) {
        // 统一从数据库状态码反查枚举，避免业务层散落 switch。
        for (ActivityStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }
}

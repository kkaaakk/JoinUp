package com.joinup.credit.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 信用分变更类型枚举。
 */
@Getter
@RequiredArgsConstructor
public enum CreditChangeTypeEnum {
    SIGNUP_BONUS(10, "报名奖励"),
    ATTEND_BONUS(20, "守约奖励"),
    LATE_CANCEL_PENALTY(30, "临时取消扣分"),
    NO_SHOW_PENALTY(40, "爽约扣分"),
    MANUAL_ADJUST(50, "运营调整");

    private final int code;
    private final String description;
}

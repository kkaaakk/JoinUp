package com.joinup.signup.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 报名来源枚举。
 */
@Getter
@RequiredArgsConstructor
public enum SignupSourceEnum {
    DIRECT(1, "直接报名"),
    WAITLIST_PROMOTION(2, "候补补位");

    private final int code;
    private final String description;
}

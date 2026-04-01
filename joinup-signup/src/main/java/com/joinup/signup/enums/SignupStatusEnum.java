package com.joinup.signup.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SignupStatusEnum {
    SIGNED(10, "已报名"),
    CANCELED(20, "已取消"),
    NO_SHOW(30, "已爽约"),
    COMPLETED(40, "已完成"),
    PROMOTED(50, "候补转正");

    private final int code;
    private final String description;
}

package com.joinup.user.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 用户性别枚举。
 */
@Getter
@RequiredArgsConstructor
public enum GenderEnum {
    UNKNOWN(0, "未知"),
    MALE(1, "男"),
    FEMALE(2, "女"),
    OTHER(3, "其他");

    private final int code;
    private final String description;
}

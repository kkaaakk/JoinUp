package com.joinup.user.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 用户状态枚举。
 */
@Getter
@RequiredArgsConstructor
public enum UserStatusEnum {
    ACTIVE(1, "正常"),
    DISABLED(2, "禁用"),
    DEACTIVATED(3, "注销");

    private final int code;
    private final String description;
}

package com.joinup.notify.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 通知发送状态枚举。
 */
@Getter
@RequiredArgsConstructor
public enum NotifyStatusEnum {
    PENDING(10, "待发送"),
    SENT(20, "发送成功"),
    FAILED(30, "发送失败"),
    READ(40, "已读");

    private final int code;
    private final String description;
}

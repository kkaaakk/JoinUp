package com.joinup.notify.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 通知渠道枚举。
 */
@Getter
@RequiredArgsConstructor
public enum NotifyChannelEnum {
    IN_APP(10, "站内信"),
    SMS(20, "短信"),
    EMAIL(30, "邮件");

    private final int code;
    private final String description;
}

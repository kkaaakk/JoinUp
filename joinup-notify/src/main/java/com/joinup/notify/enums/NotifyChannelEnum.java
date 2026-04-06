package com.joinup.notify.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 通知渠道枚举。
 * <p>
 * 当前真正落地的是站内信渠道，其余渠道先保留接口和占位实现，方便后续接短信、邮件、小程序订阅消息。
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum NotifyChannelEnum {
    IN_APP(10, "站内信"),
    SMS(20, "短信"),
    EMAIL(30, "邮件"),
    MINI_PROGRAM(40, "小程序通知");

    private final int code;
    private final String description;

    /**
     * 根据渠道编码反查枚举。
     *
     * @param code 渠道编码
     * @return 匹配到的枚举；如果没有匹配项则返回 {@code null}
     */
    public static NotifyChannelEnum fromCode(Integer code) {
        for (NotifyChannelEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }
}
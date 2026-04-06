package com.joinup.notify.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 通知发送状态枚举。
 * <p>
 * 这里同时覆盖“已发送 / 发送失败 / 已读”等状态，便于站内信分页和发送追踪共用一套状态模型。
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum NotifyStatusEnum {
    PENDING(10, "待发送"),
    SENT(20, "已发送"),
    FAILED(30, "发送失败"),
    READ(40, "已读");

    private final int code;
    private final String description;

    /**
     * 根据状态编码反查枚举。
     *
     * @param code 状态编码
     * @return 匹配到的枚举；如果没有匹配项则返回 {@code null}
     */
    public static NotifyStatusEnum fromCode(Integer code) {
        for (NotifyStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }
}
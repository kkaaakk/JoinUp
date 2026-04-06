package com.joinup.waitlist.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 候补补位来源枚举。
 * <p>
 * 该字段用于记录某次候补补位机会是怎么产生的，方便后续审计和问题排查。
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum WaitlistPromotionSourceEnum {
    FORMAL_CANCEL(10, "正式名额取消触发"),
    MANUAL_TRIGGER(20, "人工触发补位"),
    TIMEOUT_ROLLOVER(30, "上一位超时顺延");

    private final int code;
    private final String description;

    /**
     * 根据来源码反查枚举值。
     *
     * @param code 来源码
     * @return 对应来源枚举；如果未匹配到则返回 {@code null}
     */
    public static WaitlistPromotionSourceEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (WaitlistPromotionSourceEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }
}

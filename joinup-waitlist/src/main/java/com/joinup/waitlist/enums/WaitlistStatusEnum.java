package com.joinup.waitlist.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 候补状态枚举。
 * <p>
 * 候补链路中的状态区分了“排队中”“已发放补位待确认”“已确认转正”“已取消”“已超时失效”几个关键阶段，
 * 这样后续查询、超时扫描和自动顺延都能清晰判断当前记录所处的位置。
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum WaitlistStatusEnum {
    QUEUED(10, "排队中"),
    WAITING_CONFIRM(20, "待确认补位"),
    PROMOTED(30, "已确认转正"),
    CANCELED(40, "已取消"),
    EXPIRED(50, "已超时失效");

    private final int code;
    private final String description;

    /**
     * 根据状态码反查枚举值。
     *
     * @param code 状态码
     * @return 对应状态枚举；如果未匹配到则返回 {@code null}
     */
    public static WaitlistStatusEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (WaitlistStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }
}

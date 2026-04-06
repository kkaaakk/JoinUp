package com.joinup.signup.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 报名预占结果类型枚举。
 * <p>
 * 该枚举表示用户在 Redis 原子抢位阶段最终拿到的是哪一种席位：
 * 1. {@code FORMAL} 表示成功预占正式名额；
 * 2. {@code WAITLIST} 表示正式名额已满，但成功进入候补队列。
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum SignupReservationTypeEnum {
    FORMAL("FORMAL"),
    WAITLIST("WAITLIST");

    private final String code;

    /**
     * 根据编码反查预占类型。
     * <p>
     * 主要用于 Redis 缓存反序列化、消息恢复以及接口查询时的状态还原。
     * 这里忽略大小写，提升不同来源数据的兼容性。
     * </p>
     *
     * @param code 类型编码
     * @return 对应的枚举值；如果未匹配到则返回 {@code null}
     */
    public static SignupReservationTypeEnum fromCode(String code) {
        for (SignupReservationTypeEnum value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        return null;
    }
}

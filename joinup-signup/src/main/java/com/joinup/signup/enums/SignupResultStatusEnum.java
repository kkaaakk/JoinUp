package com.joinup.signup.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 报名结果统一状态枚举。
 * <p>
 * 这个状态是对外接口层使用的统一抽象，不直接等价于某一张表中的原始状态字段。
 * 它的目的是把正式报名表、候补表、Redis 处理中状态统一收口成前端更容易消费的结果集。
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum SignupResultStatusEnum {
    PROCESSING("PROCESSING"),
    SIGNED("SIGNED"),
    WAITLISTED("WAITLISTED"),
    CANCELED("CANCELED"),
    FAILED("FAILED"),
    NONE("NONE");

    private final String code;

    /**
     * 根据编码反查统一结果状态。
     * <p>
     * 主要用于 Redis 中缓存状态的恢复与查询接口结果拼装。
     * </p>
     *
     * @param code 状态编码
     * @return 对应的枚举值；如果未匹配到则返回 {@code null}
     */
    public static SignupResultStatusEnum fromCode(String code) {
        for (SignupResultStatusEnum value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        return null;
    }
}

package com.joinup.credit.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 信用限制类型枚举。
 * <p>
 * 这个枚举不直接描述“分数怎么变”，而是描述“当前信用状态会导致什么限制”。
 * 这样可以把限制判断和分值变化解耦，后续做更细的风控策略时会更容易扩展。
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum CreditRestrictionTypeEnum {
    BLOCK_HOT_ACTIVITY_SIGNUP("限制报名热门活动"),
    BLOCK_ACTIVITY_CREATE("限制创建活动"),
    DEPRIORITIZE_WAITLIST("降低候补优先级");

    private final String description;
}
package com.joinup.credit.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 信用分变更类型枚举。
 * <p>
 * 这里把“什么行为触发了信用变化”抽成枚举，避免在 service 里散落 magic number。
 * 后续如果新增“举报成立扣分”“平台补偿加分”等规则，只需要在这里扩展类型即可。
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum CreditChangeTypeEnum {
    ATTEND_BONUS(20, "正常参加活动"),
    NORMAL_CANCEL(25, "允许时间内取消"),
    LATE_CANCEL_PENALTY(30, "临时取消"),
    NO_SHOW_PENALTY(40, "爽约未到场"),
    MANUAL_ADJUST(50, "管理员人工调整");

    private final int code;
    private final String description;

    /**
     * 根据数据库中的类型编码反查枚举。
     *
     * @param code 类型编码
     * @return 匹配到的枚举；如果没有匹配项则返回 {@code null}
     */
    public static CreditChangeTypeEnum fromCode(Integer code) {
        for (CreditChangeTypeEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }
}
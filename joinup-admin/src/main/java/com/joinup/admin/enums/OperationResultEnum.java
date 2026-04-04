package com.joinup.admin.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 后台操作结果枚举。
 */
@Getter
@RequiredArgsConstructor
public enum OperationResultEnum {
    /** 操作成功。 */
    SUCCESS(10, "成功"),
    /** 操作失败。 */
    FAILED(20, "失败");

    private final int code;
    private final String description;

    /**
     * 根据数据库中的结果码反查枚举。
     * <p>
     * 分页查询操作日志时，数据库里存的是整数编码，对外展示时需要把编码转换成语义化描述。
     * </p>
     *
     * @param code 操作结果编码
     * @return 匹配到的枚举；如果没有匹配项则返回 {@code null}
     */
    public static OperationResultEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (OperationResultEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }
}
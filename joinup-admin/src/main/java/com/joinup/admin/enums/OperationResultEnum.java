package com.joinup.admin.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 操作结果枚举。
 */
@Getter
@RequiredArgsConstructor
public enum OperationResultEnum {
    SUCCESS(10, "成功"),
    FAILED(20, "失败");

    private final int code;
    private final String description;
}

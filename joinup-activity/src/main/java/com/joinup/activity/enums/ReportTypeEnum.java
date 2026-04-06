package com.joinup.activity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 举报类型枚举。
 */
@Getter
@RequiredArgsConstructor
public enum ReportTypeEnum {
    ILLEGAL_CONTENT(10, "违规内容"),
    HARASSMENT(20, "骚扰"),
    NO_SHOW(30, "爽约"),
    OTHER(40, "其他");

    private final int code;
    private final String description;
}

package com.joinup.activity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportStatusEnum {
    PENDING(10, "待处理"),
    PROCESSING(20, "处理中"),
    APPROVED(30, "已通过"),
    REJECTED(40, "已驳回");

    private final int code;
    private final String description;
}

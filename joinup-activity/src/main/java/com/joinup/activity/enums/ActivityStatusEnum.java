package com.joinup.activity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ActivityStatusEnum {
    DRAFT(10, "草稿"),
    RECRUITING(20, "报名中"),
    FULL(30, "已满员"),
    GROUPED(40, "已成团"),
    FAILED(50, "流局"),
    CANCELED(60, "已取消"),
    FINISHED(70, "已结束");

    private final int code;
    private final String description;
}

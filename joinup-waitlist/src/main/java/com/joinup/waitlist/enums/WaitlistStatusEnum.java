package com.joinup.waitlist.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WaitlistStatusEnum {
    QUEUED(10, "排队中"),
    PROMOTED(20, "已补位"),
    CANCELED(30, "已取消"),
    EXPIRED(40, "已失效");

    private final int code;
    private final String description;
}

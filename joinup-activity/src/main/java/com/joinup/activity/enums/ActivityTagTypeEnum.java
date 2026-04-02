package com.joinup.activity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ActivityTagTypeEnum {
    SYSTEM(1, "system"),
    CUSTOM(2, "custom");

    private final int code;
    private final String description;
}

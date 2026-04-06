package com.joinup.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DeletedFlagEnum {
    NO(0),
    YES(1);

    private final Integer code;
}

package com.joinup.signup.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Command types sent to Kafka for asynchronous persistence.
 */
@Getter
@RequiredArgsConstructor
public enum SignupCommandTypeEnum {
    APPLY("APPLY"),
    CANCEL("CANCEL");

    private final String code;
}
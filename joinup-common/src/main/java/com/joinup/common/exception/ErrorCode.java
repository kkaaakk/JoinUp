package com.joinup.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    SUCCESS(0, "Success"),
    BAD_REQUEST(40000, "Bad request"),
    UNAUTHORIZED(40100, "Unauthorized"),
    FORBIDDEN(40300, "Forbidden"),
    NOT_FOUND(40400, "Resource not found"),
    CONFLICT(40900, "Resource state conflict"),
    USER_ALREADY_EXISTS(41000, "User already exists"),
    INVALID_CREDENTIALS(41001, "Invalid credentials"),
    USER_DISABLED(41002, "User is disabled"),
    USER_NOT_FOUND(41003, "User not found"),
    PASSWORD_CONFIRM_MISMATCH(41004, "Password confirmation does not match"),
    INTERNAL_ERROR(50000, "Internal server error");

    private final Integer code;
    private final String message;
}

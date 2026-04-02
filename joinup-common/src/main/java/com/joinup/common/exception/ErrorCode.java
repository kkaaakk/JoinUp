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
    ACTIVITY_NOT_FOUND(42000, "Activity not found"),
    ACTIVITY_INVALID_ARGUMENT(42001, "Activity argument is invalid"),
    ACTIVITY_STATUS_CONFLICT(42002, "Activity status conflict"),
    ACTIVITY_PERMISSION_DENIED(42003, "Activity permission denied"),
    ACTIVITY_REVIEW_PERMISSION_DENIED(42004, "Activity review permission denied"),
    INTERNAL_ERROR(50000, "Internal server error");

    private final Integer code;
    private final String message;
}

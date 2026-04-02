package com.joinup.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 全局错误码定义。
 * 先按模块聚类，后续如果规模变大可以拆为分模块错误码枚举。
 */
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
    SIGNUP_ACTIVITY_NOT_OPEN(43000, "Signup activity is not open"),
    SIGNUP_DEADLINE_EXPIRED(43001, "Signup deadline has expired"),
    SIGNUP_FULL(43002, "Signup quota is full"),
    SIGNUP_WAITLIST_FULL(43003, "Signup waitlist is full"),
    SIGNUP_NOT_FOUND(43004, "Signup record not found"),
    SIGNUP_CANCEL_NOT_ALLOWED(43005, "Signup cancel is not allowed"),
    SIGNUP_EVENT_DISPATCH_FAILED(43006, "Signup event dispatch failed"),
    SIGNUP_CACHE_INITIALIZATION_FAILED(43007, "Signup cache initialization failed"),
    INTERNAL_ERROR(50000, "Internal server error");

    private final Integer code;
    private final String message;
}

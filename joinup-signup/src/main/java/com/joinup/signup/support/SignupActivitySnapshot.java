package com.joinup.signup.support;

import lombok.Builder;
import lombok.Getter;

/**
 * Cached activity snapshot used by the signup module.
 */
@Getter
@Builder
public class SignupActivitySnapshot {

    private final Long activityId;
    private final Integer status;
    private final Integer allowWaitlist;
    private final Integer formalRemaining;
    private final Integer waitlistRemaining;
    private final Integer maxQueueNo;
    private final Long signupDeadlineEpochMillis;
    private final Long endTimeEpochMillis;
}
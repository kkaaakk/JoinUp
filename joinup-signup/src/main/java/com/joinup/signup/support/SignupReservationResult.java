package com.joinup.signup.support;

import com.joinup.signup.enums.SignupReservationTypeEnum;
import lombok.Builder;
import lombok.Getter;

/**
 * Result returned from the Redis + Lua reservation step.
 */
@Getter
@Builder
public class SignupReservationResult {

    private final boolean accepted;
    private final boolean duplicateRequest;
    private final SignupReservationTypeEnum reservationType;
    private final Integer queueNo;
    private final SignupCachedState cachedState;
}
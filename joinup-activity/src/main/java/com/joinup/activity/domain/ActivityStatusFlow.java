package com.joinup.activity.domain;

import com.joinup.activity.enums.ActivityStatusEnum;
import com.joinup.common.exception.BusinessException;
import com.joinup.common.exception.ErrorCode;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class ActivityStatusFlow {

    private static final Map<ActivityStatusEnum, Set<ActivityStatusEnum>> ALLOWED_TRANSITIONS = Map.of(
            ActivityStatusEnum.DRAFT, EnumSet.of(ActivityStatusEnum.PENDING_REVIEW, ActivityStatusEnum.CANCELED),
            ActivityStatusEnum.PENDING_REVIEW, EnumSet.of(ActivityStatusEnum.DRAFT, ActivityStatusEnum.SIGNUP_OPEN, ActivityStatusEnum.CANCELED),
            ActivityStatusEnum.SIGNUP_OPEN, EnumSet.of(ActivityStatusEnum.FULL, ActivityStatusEnum.WAITLIST_OPEN,
                    ActivityStatusEnum.GROUP_SUCCESS, ActivityStatusEnum.GROUP_FAILED, ActivityStatusEnum.CANCELED),
            ActivityStatusEnum.FULL, EnumSet.of(ActivityStatusEnum.WAITLIST_OPEN, ActivityStatusEnum.GROUP_SUCCESS,
                    ActivityStatusEnum.GROUP_FAILED, ActivityStatusEnum.CANCELED),
            ActivityStatusEnum.WAITLIST_OPEN, EnumSet.of(ActivityStatusEnum.GROUP_SUCCESS, ActivityStatusEnum.GROUP_FAILED,
                    ActivityStatusEnum.CANCELED),
            ActivityStatusEnum.GROUP_SUCCESS, EnumSet.of(ActivityStatusEnum.IN_PROGRESS, ActivityStatusEnum.CANCELED),
            ActivityStatusEnum.GROUP_FAILED, EnumSet.of(ActivityStatusEnum.CANCELED),
            ActivityStatusEnum.IN_PROGRESS, EnumSet.of(ActivityStatusEnum.FINISHED, ActivityStatusEnum.CANCELED),
            ActivityStatusEnum.FINISHED, EnumSet.noneOf(ActivityStatusEnum.class),
            ActivityStatusEnum.CANCELED, EnumSet.noneOf(ActivityStatusEnum.class)
    );

    private ActivityStatusFlow() {
    }

    public static ActivityStatusEnum initialStatus() {
        return ActivityStatusEnum.PENDING_REVIEW;
    }

    public static void assertCanEdit(ActivityStatusEnum status, Integer currentParticipants, Integer waitlistCount) {
        if (status == ActivityStatusEnum.DRAFT || status == ActivityStatusEnum.PENDING_REVIEW) {
            return;
        }
        if ((status == ActivityStatusEnum.SIGNUP_OPEN || status == ActivityStatusEnum.WAITLIST_OPEN)
                && safeInt(currentParticipants) == 0
                && safeInt(waitlistCount) == 0) {
            return;
        }
        throw new BusinessException(ErrorCode.ACTIVITY_STATUS_CONFLICT, "Activity cannot be edited in current status");
    }

    public static void assertCanCancel(ActivityStatusEnum status) {
        if (status == ActivityStatusEnum.FINISHED || status == ActivityStatusEnum.CANCELED) {
            throw new BusinessException(ErrorCode.ACTIVITY_STATUS_CONFLICT, "Activity cannot be canceled in current status");
        }
    }

    public static ActivityStatusEnum nextStatusOnReview(boolean approved) {
        return approved ? ActivityStatusEnum.SIGNUP_OPEN : ActivityStatusEnum.DRAFT;
    }

    public static void assertTransition(ActivityStatusEnum from, ActivityStatusEnum to) {
        Set<ActivityStatusEnum> targets = ALLOWED_TRANSITIONS.getOrDefault(from, EnumSet.noneOf(ActivityStatusEnum.class));
        if (!targets.contains(to)) {
            throw new BusinessException(ErrorCode.ACTIVITY_STATUS_CONFLICT,
                    "Illegal activity status transition: " + from + " -> " + to);
        }
    }

    private static int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}

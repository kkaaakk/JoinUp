package com.joinup.waitlist.vo;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 活动候补页视图对象。
 */
@Getter
@Builder
public class WaitlistActivityVO {

    private final Long activityId;
    private final String activityTitle;
    private final Integer totalQueued;
    private final String currentUserStatus;
    private final Integer currentUserQueueNo;
    private final Integer currentUserRank;
    private final LocalDateTime currentUserConfirmDeadline;
    private final java.util.List<WaitlistQueueItemVO> queueItems;
}

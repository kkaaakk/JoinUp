package com.joinup.waitlist.vo;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 我的候补列表项视图对象。
 */
@Getter
@Builder
public class WaitlistMyItemVO {

    private final Long activityId;
    private final String activityTitle;
    private final String status;
    private final Integer queueNo;
    private final Integer currentRank;
    private final LocalDateTime joinedAt;
    private final LocalDateTime promotedAt;
    private final LocalDateTime confirmedAt;
    private final LocalDateTime confirmDeadline;
    private final LocalDateTime expiredAt;
}

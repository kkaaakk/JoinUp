package com.joinup.waitlist.vo;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 候补队列活动页列表项。
 */
@Getter
@Builder
public class WaitlistQueueItemVO {

    private final Long userId;
    private final Integer queueNo;
    private final LocalDateTime joinedAt;
}

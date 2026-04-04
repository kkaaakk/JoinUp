package com.joinup.admin.vo;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * 热门活动监控单条数据视图。
 */
@Value
@Builder
public class AdminHotActivityItemVO {
    Long activityId;
    String title;
    String category;
    Long organizerId;
    String organizerUsername;
    Integer status;
    String statusDescription;
    Integer heatScore;
    Long viewCount;
    Integer currentParticipants;
    Integer maxParticipants;
    LocalDateTime signupDeadline;
}
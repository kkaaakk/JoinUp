package com.joinup.activity.vo;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ActivityPageItemVO {

    private Long id;
    private Long organizerId;
    private String title;
    private String activityType;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime signupDeadline;
    private Integer maxParticipants;
    private Integer minGroupSize;
    private Integer currentParticipants;
    private Integer allowWaitlist;
    private Integer waitlistLimit;
    private Integer status;
    private String statusDescription;
    private Long viewCount;
    private Integer heatScore;
    private List<String> tags;
}

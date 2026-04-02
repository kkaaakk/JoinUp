package com.joinup.activity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class ActivityPageQuery {

    @Min(1)
    private long current = 1;

    @Min(1)
    @Max(100)
    private long size = 10;

    private Integer status;

    private Long organizerId;

    private String activityType;

    private String keyword;

    private Boolean allowWaitlist;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTimeFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTimeTo;
}

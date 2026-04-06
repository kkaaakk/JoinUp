package com.joinup.activity.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建活动请求。
 */
@Data
public class ActivityCreateRequest {

    /** 活动标题。 */
    @NotBlank
    @Size(max = 128)
    private String title;

    /** 活动类别，如羽毛球、骑行、自习等。 */
    @NotBlank
    @Size(max = 32)
    private String activityType;

    @Size(max = 2000)
    private String description;

    @NotBlank
    @Size(max = 255)
    private String location;

    @NotNull
    @Future
    private LocalDateTime startTime;

    @NotNull
    @Future
    private LocalDateTime endTime;

    @NotNull
    @Future
    private LocalDateTime signupDeadline;

    @NotNull
    @Positive
    private Integer maxParticipants;

    @NotNull
    @Positive
    private Integer minGroupSize;

    /** 是否允许报名满员后进入候补。 */
    @NotNull
    private Boolean allowWaitlist;

    /** 候补队列上限，仅在允许候补时生效。 */
    @PositiveOrZero
    private Integer waitlistLimit;

    /** 活动标签，后续可用于推荐与检索。 */
    @NotEmpty
    @Size(max = 8)
    private List<@NotBlank @Size(max = 32) String> tags;
}

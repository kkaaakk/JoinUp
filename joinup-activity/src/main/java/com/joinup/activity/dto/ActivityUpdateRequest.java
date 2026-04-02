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
 * 编辑活动请求。
 */
@Data
public class ActivityUpdateRequest {

    @NotBlank
    @Size(max = 128)
    private String title;

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

    @NotNull
    private Boolean allowWaitlist;

    @PositiveOrZero
    private Integer waitlistLimit;

    @NotEmpty
    @Size(max = 8)
    private List<@NotBlank @Size(max = 32) String> tags;
}

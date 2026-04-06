package com.joinup.signup.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Result view for the current user on a specific activity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupActivityResultVO {

    private Long activityId;
    private String activityTitle;
    private Long userId;
    private String requestId;
    private String resultStatus;
    private String reservationType;
    private String signupStatus;
    private String waitlistStatus;
    private Integer queueNo;
    private String message;
    private LocalDateTime updatedAt;
}
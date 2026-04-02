package com.joinup.signup.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Row returned by GET /api/signup/my.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupMyItemVO {

    private Long activityId;
    private String activityTitle;
    private String resultStatus;
    private String reservationType;
    private String signupStatus;
    private String waitlistStatus;
    private Integer queueNo;
    private LocalDateTime signupTime;
    private LocalDateTime cancelTime;
    private LocalDateTime updatedAt;
}
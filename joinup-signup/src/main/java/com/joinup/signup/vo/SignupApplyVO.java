package com.joinup.signup.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response returned immediately after apply/cancel submission.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupApplyVO {

    private String requestId;
    private Long activityId;
    private String resultStatus;
    private String reservationType;
    private Integer queueNo;
    private Boolean duplicateRequest;
    private String message;
}
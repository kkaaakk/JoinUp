package com.joinup.credit.vo;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 用户信用记录展示对象。
 */
@Getter
@Builder
public class UserCreditRecordVO {

    private Long id;
    private Integer changeType;
    private String changeTypeDescription;
    private Integer deltaScore;
    private Integer beforeScore;
    private Integer afterScore;
    private String reason;
    private Long relatedActivityId;
    private Long relatedSignupId;
    private LocalDateTime createdAt;
}
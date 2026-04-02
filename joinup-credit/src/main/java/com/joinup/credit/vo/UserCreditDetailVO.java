package com.joinup.credit.vo;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 用户信用详情展示对象。
 */
@Getter
@Builder
public class UserCreditDetailVO {

    private Long userId;
    private Integer creditScore;
    private Integer consecutiveNoShowCount;
    private Boolean allowCreateActivity;
    private Boolean allowHotActivitySignup;
    private Boolean deprioritizeWaitlist;
    private List<CreditRestrictionVO> restrictions;
    private List<UserCreditRecordVO> recentRecords;
}
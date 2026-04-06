package com.joinup.credit.support;

import com.joinup.credit.enums.CreditRestrictionTypeEnum;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 信用限制判断结果。
 * <p>
 * 这个对象是领域层内部的“判断快照”，既能给接口层组装 VO，也能给其他业务模块做准入判断。
 * </p>
 */
@Getter
@Builder
public class CreditRestrictionDecision {

    private Long userId;
    private Integer creditScore;
    private Integer consecutiveNoShowCount;
    private boolean allowCreateActivity;
    private boolean allowHotActivitySignup;
    private boolean deprioritizeWaitlist;
    private List<CreditRestrictionTypeEnum> restrictionTypes;
}
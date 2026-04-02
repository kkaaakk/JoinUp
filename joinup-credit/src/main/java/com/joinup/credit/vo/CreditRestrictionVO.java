package com.joinup.credit.vo;

import lombok.Builder;
import lombok.Getter;

/**
 * 信用限制展示对象。
 */
@Getter
@Builder
public class CreditRestrictionVO {

    private String type;
    private String description;
}
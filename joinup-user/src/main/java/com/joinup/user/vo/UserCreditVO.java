package com.joinup.user.vo;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserCreditVO {

    private Long userId;
    private Integer creditScore;
    private Integer status;
    private String statusDescription;
}

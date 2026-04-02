package com.joinup.user.vo;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserRegisterVO {

    private Long userId;
    private String username;
    private String nickname;
    private Integer status;
    private String statusDescription;
    private Integer creditScore;
}

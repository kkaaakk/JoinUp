package com.joinup.common.context;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginUser {

    private Long userId;
    private String nickname;
    private Integer creditScore;
}

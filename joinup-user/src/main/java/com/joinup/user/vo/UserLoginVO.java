package com.joinup.user.vo;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserLoginVO {

    private String accessToken;
    private String tokenType;
    private Long expiresInSeconds;
    private UserProfileVO profile;
}

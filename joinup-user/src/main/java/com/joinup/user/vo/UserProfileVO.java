package com.joinup.user.vo;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class UserProfileVO {

    private Long userId;
    private String username;
    private String phone;
    private String email;
    private Integer status;
    private String statusDescription;
    private Integer creditScore;
    private LocalDateTime lastLoginAt;
    private String nickname;
    private String avatarUrl;
    private Integer gender;
    private String genderDescription;
    private LocalDate birthday;
    private String schoolName;
    private String major;
    private String bio;
    private String city;
}

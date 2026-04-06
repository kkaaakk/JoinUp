package com.joinup.admin.vo;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * 风险用户监控单条数据视图。
 */
@Value
@Builder
public class AdminRiskUserItemVO {
    Long userId;
    String username;
    String phone;
    String email;
    Integer status;
    String statusDescription;
    Integer creditScore;
    String riskReason;
    LocalDateTime lastLoginAt;
    LocalDateTime updatedAt;
}
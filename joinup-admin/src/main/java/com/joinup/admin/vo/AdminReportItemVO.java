package com.joinup.admin.vo;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * 举报列表单条数据视图。
 */
@Value
@Builder
public class AdminReportItemVO {
    Long id;
    Long activityId;
    String activityTitle;
    Long reporterUserId;
    String reporterUsername;
    Long reportedUserId;
    String reportedUsername;
    Integer reportType;
    String reportTypeDescription;
    String reason;
    String evidenceUrls;
    Integer status;
    String statusDescription;
    Long handlerUserId;
    String handlerUsername;
    String handleResult;
    LocalDateTime handledAt;
    LocalDateTime createdAt;
}
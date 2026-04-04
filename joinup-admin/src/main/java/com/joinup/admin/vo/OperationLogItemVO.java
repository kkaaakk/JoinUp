package com.joinup.admin.vo;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * 操作日志单条记录视图。
 */
@Value
@Builder
public class OperationLogItemVO {
    Long id;
    Long operatorId;
    String operatorRole;
    String moduleName;
    String operationType;
    String bizType;
    Long bizId;
    Integer operationResult;
    String operationResultDescription;
    String errorMessage;
    String extraData;
    LocalDateTime operationTime;
}
package com.joinup.admin.vo;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * 操作日志分页结果视图。
 */
@Value
@Builder
public class OperationLogPageVO {
    Long current;
    Long size;
    Long total;
    List<OperationLogItemVO> records;
}
package com.joinup.admin.vo;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * 风险用户监控分页结果。
 */
@Value
@Builder
public class AdminRiskUserPageVO {
    Long current;
    Long size;
    Long total;
    List<AdminRiskUserItemVO> records;
}
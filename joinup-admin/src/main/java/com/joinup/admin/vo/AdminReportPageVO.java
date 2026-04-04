package com.joinup.admin.vo;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * 举报分页结果视图。
 */
@Value
@Builder
public class AdminReportPageVO {
    Long current;
    Long size;
    Long total;
    List<AdminReportItemVO> records;
}
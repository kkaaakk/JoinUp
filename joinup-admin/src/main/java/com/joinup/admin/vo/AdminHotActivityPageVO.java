package com.joinup.admin.vo;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * 热门活动监控分页结果。
 */
@Value
@Builder
public class AdminHotActivityPageVO {
    Long current;
    Long size;
    Long total;
    List<AdminHotActivityItemVO> records;
}
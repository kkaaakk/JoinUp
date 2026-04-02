package com.joinup.notify.vo;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 通知分页结果。
 */
@Getter
@Builder
public class NotifyPageVO {

    private Long total;
    private Long pageNum;
    private Long pageSize;
    private List<NotifyMessageItemVO> records;
}
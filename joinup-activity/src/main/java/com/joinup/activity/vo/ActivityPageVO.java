package com.joinup.activity.vo;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ActivityPageVO {

    private long current;
    private long size;
    private long total;
    private List<ActivityPageItemVO> records;
}

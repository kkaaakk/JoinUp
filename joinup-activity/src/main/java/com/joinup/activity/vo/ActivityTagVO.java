package com.joinup.activity.vo;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ActivityTagVO {

    private String tagName;
    private Integer tagType;
}

package com.joinup.notify.vo;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 单条通知展示对象。
 */
@Getter
@Builder
public class NotifyMessageItemVO {

    private Long id;
    private Integer messageType;
    private String messageTypeDescription;
    private Integer channel;
    private String channelDescription;
    private String title;
    private String content;
    private String bizType;
    private Long bizId;
    private Integer status;
    private String statusDescription;
    private LocalDateTime sendTime;
    private LocalDateTime readTime;
}
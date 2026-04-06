package com.joinup.notify.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.joinup.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 通知消息实体。
 * <p>
 * 该实体统一承载站内信和未来可扩展的短信、邮件、小程序通知发送记录。
 * 当前用户侧分页和已读接口主要面向 `IN_APP` 站内信渠道。
 * </p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notify_message")
public class NotifyMessageEntity extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("message_type")
    private Integer messageType;

    @TableField("channel")
    private Integer channel;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;

    @TableField("biz_type")
    private String bizType;

    @TableField("biz_id")
    private Long bizId;

    @TableField("status")
    private Integer status;

    @TableField("send_time")
    private LocalDateTime sendTime;

    @TableField("read_time")
    private LocalDateTime readTime;

    @TableField("fail_reason")
    private String failReason;
}
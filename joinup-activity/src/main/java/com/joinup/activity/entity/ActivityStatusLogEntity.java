package com.joinup.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.joinup.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 活动状态流转日志实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("activity_status_log")
public class ActivityStatusLogEntity extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("activity_id")
    private Long activityId;

    @TableField("from_status")
    private Integer fromStatus;

    @TableField("to_status")
    private Integer toStatus;

    @TableField("reason")
    private String reason;

    @TableField("operator_id")
    private Long operatorId;

    @TableField("occurred_at")
    private LocalDateTime occurredAt;
}

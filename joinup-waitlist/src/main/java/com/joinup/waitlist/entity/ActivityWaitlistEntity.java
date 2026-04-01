package com.joinup.waitlist.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.joinup.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("activity_waitlist")
public class ActivityWaitlistEntity extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("activity_id")
    private Long activityId;

    @TableField("user_id")
    private Long userId;

    @TableField("queue_no")
    private Integer queueNo;

    @TableField("status")
    private Integer status;

    @TableField("joined_at")
    private LocalDateTime joinedAt;

    @TableField("promoted_at")
    private LocalDateTime promotedAt;

    @TableField("expired_at")
    private LocalDateTime expiredAt;

    @Version
    @TableField("version")
    private Integer version;
}

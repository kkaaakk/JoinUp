package com.joinup.signup.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.joinup.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 活动报名实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("activity_signup")
public class ActivitySignupEntity extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("activity_id")
    private Long activityId;

    @TableField("user_id")
    private Long userId;

    @TableField("status")
    private Integer status;

    /** 报名来源，区分直接报名与候补补位。 */
    @TableField("source")
    private Integer source;

    @TableField("signup_time")
    private LocalDateTime signupTime;

    @TableField("cancel_time")
    private LocalDateTime cancelTime;

    @TableField("cancel_reason")
    private String cancelReason;

    @Version
    /** 乐观锁版本，预留给高并发报名场景。 */
    @TableField("version")
    private Integer version;
}

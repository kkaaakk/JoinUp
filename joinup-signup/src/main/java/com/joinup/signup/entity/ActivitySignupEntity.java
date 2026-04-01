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

    @TableField("source")
    private Integer source;

    @TableField("signup_time")
    private LocalDateTime signupTime;

    @TableField("cancel_time")
    private LocalDateTime cancelTime;

    @TableField("cancel_reason")
    private String cancelReason;

    @Version
    @TableField("version")
    private Integer version;
}

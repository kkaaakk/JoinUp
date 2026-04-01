package com.joinup.credit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.joinup.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_credit_record")
public class UserCreditRecordEntity extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("change_type")
    private Integer changeType;

    @TableField("delta_score")
    private Integer deltaScore;

    @TableField("before_score")
    private Integer beforeScore;

    @TableField("after_score")
    private Integer afterScore;

    @TableField("reason")
    private String reason;

    @TableField("related_activity_id")
    private Long relatedActivityId;

    @TableField("related_signup_id")
    private Long relatedSignupId;
}

package com.joinup.credit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.joinup.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户信用分变更记录实体。
 * <p>
 * 这张表承担两类职责：
 * </p>
 * <p>
 * 1. 记录每一次信用分变化的审计轨迹，便于追溯“为什么变成这个分数”。
 * </p>
 * <p>
 * 2. 为后续风控判断提供行为序列，例如统计连续爽约次数、识别人工调整等。
 * </p>
 */
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

    /** 本次变更分值，可正可负，也允许为 0。 */
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
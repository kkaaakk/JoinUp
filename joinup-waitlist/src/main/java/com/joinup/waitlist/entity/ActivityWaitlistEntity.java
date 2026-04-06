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

/**
 * 活动候补实体。
 * <p>
 * 该实体用于承载候补用户从入队、获得补位机会、确认转正到超时失效的完整生命周期。
 * </p>
 */
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

    /** 候补顺序号，越小表示越靠前。 */
    @TableField("queue_no")
    private Integer queueNo;

    /** 候补记录当前状态。 */
    @TableField("status")
    private Integer status;

    /** 用户进入候补队列的时间。 */
    @TableField("joined_at")
    private LocalDateTime joinedAt;

    /** 系统发放补位机会的时间。 */
    @TableField("promoted_at")
    private LocalDateTime promotedAt;

    /** 候补用户确认补位成功的时间。 */
    @TableField("confirmed_at")
    private LocalDateTime confirmedAt;

    /** 补位确认截止时间，超过该时间未确认会被顺延。 */
    @TableField("confirm_deadline")
    private LocalDateTime confirmDeadline;

    /** 候补失效时间，例如超时失效或系统判定失效。 */
    @TableField("expired_at")
    private LocalDateTime expiredAt;

    /** 补位来源，用于标记是正式取消触发还是超时顺延触发。 */
    @TableField("promotion_source")
    private Integer promotionSource;

    @Version
    /** 乐观锁版本号，用于候补补位并发更新控制。 */
    @TableField("version")
    private Integer version;
}

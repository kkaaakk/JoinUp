package com.joinup.activity.entity;

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
 * 活动主表实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("activity")
public class ActivityEntity extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("organizer_id")
    private Long organizerId;

    @TableField("title")
    private String title;

    @TableField("description")
    private String description;

    @TableField("category")
    private String category;

    @TableField("location_text")
    private String locationText;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("signup_deadline")
    private LocalDateTime signupDeadline;

    @TableField("max_participants")
    private Integer maxParticipants;

    @TableField("min_group_size")
    private Integer minGroupSize;

    @TableField("current_participants")
    private Integer currentParticipants;

    /** 当前候补队列人数，用于快速判断是否还能继续排队。 */
    @TableField("waitlist_count")
    private Integer waitlistCount;

    /** 0/1 标记，保留为数据库整型便于和 SQL 条件直接配合。 */
    @TableField("allow_waitlist")
    private Integer allowWaitlist;

    @TableField("waitlist_limit")
    private Integer waitlistLimit;

    @TableField("status")
    private Integer status;

    /** 浏览量预留给详情页曝光统计。 */
    @TableField("view_count")
    private Long viewCount;

    /** 热度分预留给推荐、排序和运营榜单。 */
    @TableField("heat_score")
    private Integer heatScore;

    @TableField("cancel_reason")
    private String cancelReason;

    @TableField("reviewed_by")
    private Long reviewedBy;

    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;

    @TableField("review_remark")
    private String reviewRemark;

    @Version
    /** 乐观锁版本，预留给并发更新人数、热度等场景。 */
    @TableField("version")
    private Integer version;
}

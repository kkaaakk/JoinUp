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

    @TableField("waitlist_count")
    private Integer waitlistCount;

    @TableField("allow_waitlist")
    private Integer allowWaitlist;

    @TableField("status")
    private Integer status;

    @TableField("heat_score")
    private Integer heatScore;

    @TableField("cancel_reason")
    private String cancelReason;

    @Version
    @TableField("version")
    private Integer version;
}

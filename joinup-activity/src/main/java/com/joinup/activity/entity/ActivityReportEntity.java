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
 * 活动举报实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("activity_report")
public class ActivityReportEntity extends BaseEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("activity_id")
    private Long activityId;

    @TableField("reporter_user_id")
    private Long reporterUserId;

    @TableField("reported_user_id")
    private Long reportedUserId;

    @TableField("report_type")
    private Integer reportType;

    @TableField("reason")
    private String reason;

    @TableField("evidence_urls")
    private String evidenceUrls;

    @TableField("status")
    private Integer status;

    @TableField("handler_user_id")
    private Long handlerUserId;

    @TableField("handle_result")
    private String handleResult;

    @TableField("handled_at")
    private LocalDateTime handledAt;
}

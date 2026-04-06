package com.joinup.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 管理后台举报分页查询参数。
 */
@Data
public class AdminReportPageQuery {

    /** 当前页码。 */
    @Min(value = 1, message = "页码不能小于 1")
    private Long current = 1L;

    /** 每页条数。 */
    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 100, message = "每页条数不能超过 100")
    private Long size = 10L;

    /** 举报状态筛选。 */
    private Integer status;

    /** 举报类型筛选。 */
    private Integer reportType;

    /** 活动 ID 筛选。 */
    private Long activityId;

    /** 举报人 ID 筛选。 */
    private Long reporterUserId;

    /** 被举报用户 ID 筛选。 */
    private Long reportedUserId;
}
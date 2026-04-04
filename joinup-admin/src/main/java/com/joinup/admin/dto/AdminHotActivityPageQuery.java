package com.joinup.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 热门活动监控分页查询参数。
 */
@Data
public class AdminHotActivityPageQuery {

    /** 当前页码。 */
    @Min(value = 1, message = "页码不能小于 1")
    private Long current = 1L;

    /** 每页条数。 */
    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 100, message = "每页条数不能超过 100")
    private Long size = 10L;

    /** 最低热度分筛选。 */
    private Integer minHeatScore;
}
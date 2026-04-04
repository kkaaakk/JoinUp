package com.joinup.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 风险用户监控分页查询参数。
 */
@Data
public class AdminRiskUserPageQuery {

    /** 当前页码。 */
    @Min(value = 1, message = "页码不能小于 1")
    private Long current = 1L;

    /** 每页条数。 */
    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 100, message = "每页条数不能超过 100")
    private Long size = 10L;

    /**
     * 最大信用分筛选。
     * <p>
     * 为空时会使用信用模块里“热门活动报名最低分”的阈值作为默认风险阈值。
     * </p>
     */
    private Integer maxCreditScore;

    /** 是否包含已禁用用户。 */
    private Boolean includeDisabled = Boolean.TRUE;
}
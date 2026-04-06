package com.joinup.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 操作日志分页查询参数。
 */
@Data
public class OperationLogPageQuery {

    /** 当前页码。 */
    @Min(value = 1, message = "页码不能小于 1")
    private Long current = 1L;

    /** 每页条数。 */
    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 100, message = "每页条数不能超过 100")
    private Long size = 10L;

    /** 模块名称筛选。 */
    private String moduleName;

    /** 操作类型筛选。 */
    private String operationType;

    /** 操作人 ID 筛选。 */
    private Long operatorId;

    /** 操作结果筛选。 */
    private Integer operationResult;
}
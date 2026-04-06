package com.joinup.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员处理举报请求。
 */
@Data
public class AdminReportHandleRequest {

    /**
     * 举报处理后的目标状态。
     * <p>
     * 这里建议传入 {@code 30=APPROVED} 或 {@code 40=REJECTED}，
     * 具体合法性由 Service 层统一校验。
     * </p>
     */
    @NotNull(message = "处理结果状态不能为空")
    private Integer targetStatus;

    /**
     * 处理说明。
     * <p>
     * 这里会直接沉淀到举报记录和操作日志里，方便后续复盘，因此不建议留空。
     * </p>
     */
    @NotBlank(message = "处理说明不能为空")
    @Size(max = 500, message = "处理说明长度不能超过 500")
    private String handleResult;

    /** 是否同时下架被举报活动。 */
    private Boolean offShelfActivity = Boolean.FALSE;

    /** 是否同时禁用被举报用户。 */
    private Boolean disableReportedUser = Boolean.FALSE;
}
package com.joinup.credit.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员人工调整信用分请求。
 */
@Data
public class AdminCreditAdjustRequest {

    /**
     * 本次人工调整分值。
     * <p>
     * 正数代表加分，负数代表扣分。
     * 这里先用一个相对保守的校验区间兜底，真正的阈值仍由配置项控制。
     * </p>
     */
    @NotNull(message = "调整分值不能为空")
    @Min(value = -100, message = "调整分值不能小于 -100")
    @Max(value = 100, message = "调整分值不能大于 100")
    private Integer deltaScore;

    /**
     * 人工调整原因。
     */
    @NotBlank(message = "调整原因不能为空")
    @Size(max = 255, message = "调整原因长度不能超过 255")
    private String reason;
}
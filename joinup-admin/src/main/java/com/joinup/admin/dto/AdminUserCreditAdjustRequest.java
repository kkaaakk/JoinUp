package com.joinup.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理后台统一信用调整请求。
 * <p>
 * 这个对象面向 joinup-admin 的统一接口，和信用模块内部的调整 DTO 分开定义，
 * 目的是避免管理后台接口路径和信用模块内部契约强绑定。
 * </p>
 */
@Data
public class AdminUserCreditAdjustRequest {

    /** 被调整用户 ID。 */
    @NotNull(message = "目标用户 ID 不能为空")
    private Long userId;

    /** 调整分值，正数加分，负数扣分。 */
    @NotNull(message = "调整分值不能为空")
    @Min(value = -100, message = "调整分值不能小于 -100")
    @Max(value = 100, message = "调整分值不能大于 100")
    private Integer deltaScore;

    /** 调整原因。 */
    @NotBlank(message = "调整原因不能为空")
    @Size(max = 255, message = "调整原因长度不能超过 255")
    private String reason;
}
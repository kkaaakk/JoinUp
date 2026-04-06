package com.joinup.credit.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 报名前信用校验请求。
 * <p>
 * 这个对象给报名模块调用信用模块时使用，不直接面向前端接口。
 * 通过把活动热度、活动 ID 等信息显式收拢成一个对象，后续如果新增“仅某些活动类型受限”时会更容易扩展。
 * </p>
 */
@Data
public class CreditSignupCheckRequest {

    /** 当前尝试报名的用户 ID。 */
    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    /** 被报名的活动 ID。 */
    @NotNull(message = "活动 ID 不能为空")
    private Long activityId;

    /** 活动热度分，用于判断是否属于热门活动。 */
    @NotNull(message = "活动热度不能为空")
    private Integer activityHeatScore;
}
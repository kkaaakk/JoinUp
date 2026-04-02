package com.joinup.credit.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 爽约扣分请求。
 */
@Data
public class CreditNoShowPenaltyRequest {

    /** 爽约用户 ID。 */
    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    /** 关联的活动 ID。 */
    @NotNull(message = "活动 ID 不能为空")
    private Long activityId;

    /** 关联的报名记录 ID。 */
    private Long signupId;

    /** 爽约说明，便于后续申诉和审计。 */
    @Size(max = 255, message = "原因长度不能超过 255")
    private String reason;
}
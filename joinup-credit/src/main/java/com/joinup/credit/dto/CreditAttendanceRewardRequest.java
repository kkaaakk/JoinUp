package com.joinup.credit.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 正常参加活动后的信用加分请求。
 */
@Data
public class CreditAttendanceRewardRequest {

    /** 发生信用变化的用户 ID。 */
    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    /** 关联的活动 ID。 */
    @NotNull(message = "活动 ID 不能为空")
    private Long activityId;

    /** 关联的报名记录 ID。 */
    private Long signupId;

    /** 允许业务方补充更细的说明，例如“活动已完结并完成签到”。 */
    @Size(max = 255, message = "原因长度不能超过 255")
    private String reason;
}
package com.joinup.credit.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户取消报名后的信用处理请求。
 */
@Data
public class CreditCancelRequest {

    /** 发生取消行为的用户 ID。 */
    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    /** 被取消的活动 ID。 */
    @NotNull(message = "活动 ID 不能为空")
    private Long activityId;

    /** 关联的报名记录 ID。 */
    private Long signupId;

    /** 活动开始时间，用于判断是否已经进入临时取消窗口。 */
    @NotNull(message = "活动开始时间不能为空")
    private LocalDateTime activityStartTime;

    /** 实际取消时间。 */
    @NotNull(message = "取消时间不能为空")
    private LocalDateTime cancelTime;

    /** 取消原因，业务方可以原样透传。 */
    @Size(max = 255, message = "原因长度不能超过 255")
    private String reason;
}
package com.joinup.activity.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 活动审核请求。
 */
@Data
public class ActivityReviewRequest {

    /** true 表示通过，false 表示驳回。 */
    @NotNull
    private Boolean approved;

    /** 审核备注，便于回溯原因。 */
    @Size(max = 255)
    private String remark;
}

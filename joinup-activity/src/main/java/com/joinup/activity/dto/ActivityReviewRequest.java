package com.joinup.activity.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ActivityReviewRequest {

    @NotNull
    private Boolean approved;

    @Size(max = 255)
    private String remark;
}

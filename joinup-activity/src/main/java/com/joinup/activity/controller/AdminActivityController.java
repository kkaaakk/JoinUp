package com.joinup.activity.controller;

import com.joinup.activity.dto.ActivityReviewRequest;
import com.joinup.activity.service.ActivityService;
import com.joinup.activity.vo.ActivityDetailVO;
import com.joinup.common.context.LoginUser;
import com.joinup.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/activity")
public class AdminActivityController {

    private final ActivityService activityService;

    public AdminActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @PostMapping("/review/{id}")
    public Result<ActivityDetailVO> reviewActivity(@AuthenticationPrincipal LoginUser loginUser,
                                                   @PathVariable("id") Long id,
                                                   @Valid @RequestBody ActivityReviewRequest request) {
        return Result.success(activityService.reviewActivity(loginUser, id, request));
    }
}

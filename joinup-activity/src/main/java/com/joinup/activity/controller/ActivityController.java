package com.joinup.activity.controller;

import com.joinup.activity.dto.ActivityCreateRequest;
import com.joinup.activity.dto.ActivityPageQuery;
import com.joinup.activity.dto.ActivityUpdateRequest;
import com.joinup.activity.service.ActivityService;
import com.joinup.activity.vo.ActivityDetailVO;
import com.joinup.activity.vo.ActivityPageVO;
import com.joinup.common.context.LoginUser;
import com.joinup.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/activity")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @PostMapping("/create")
    public Result<ActivityDetailVO> createActivity(@AuthenticationPrincipal LoginUser loginUser,
                                                   @Valid @RequestBody ActivityCreateRequest request) {
        return Result.success(activityService.createActivity(loginUser, request));
    }

    @PutMapping("/update/{id}")
    public Result<ActivityDetailVO> updateActivity(@AuthenticationPrincipal LoginUser loginUser,
                                                   @PathVariable("id") Long id,
                                                   @Valid @RequestBody ActivityUpdateRequest request) {
        return Result.success(activityService.updateActivity(loginUser, id, request));
    }

    @GetMapping("/{id}")
    public Result<ActivityDetailVO> getActivityDetail(@PathVariable("id") Long id) {
        return Result.success(activityService.getActivityDetail(id));
    }

    @GetMapping("/page")
    public Result<ActivityPageVO> pageActivities(@Valid ActivityPageQuery query) {
        return Result.success(activityService.pageActivities(query));
    }

    @PostMapping("/cancel/{id}")
    public Result<ActivityDetailVO> cancelActivity(@AuthenticationPrincipal LoginUser loginUser,
                                                   @PathVariable("id") Long id,
                                                   @RequestParam(value = "reason", required = false) String reason) {
        return Result.success(activityService.cancelActivity(loginUser, id, reason));
    }
}

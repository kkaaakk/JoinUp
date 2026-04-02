package com.joinup.activity.service;

import com.joinup.activity.dto.ActivityCreateRequest;
import com.joinup.activity.dto.ActivityPageQuery;
import com.joinup.activity.dto.ActivityReviewRequest;
import com.joinup.activity.dto.ActivityUpdateRequest;
import com.joinup.activity.vo.ActivityDetailVO;
import com.joinup.activity.vo.ActivityPageVO;
import com.joinup.common.context.LoginUser;

public interface ActivityService {

    ActivityDetailVO createActivity(LoginUser loginUser, ActivityCreateRequest request);

    ActivityDetailVO updateActivity(LoginUser loginUser, Long activityId, ActivityUpdateRequest request);

    ActivityDetailVO getActivityDetail(Long activityId);

    ActivityPageVO pageActivities(ActivityPageQuery query);

    ActivityDetailVO cancelActivity(LoginUser loginUser, Long activityId, String reason);

    ActivityDetailVO reviewActivity(LoginUser loginUser, Long activityId, ActivityReviewRequest request);
}

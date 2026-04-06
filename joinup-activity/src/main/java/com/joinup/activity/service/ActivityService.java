package com.joinup.activity.service;

import com.joinup.activity.dto.ActivityCreateRequest;
import com.joinup.activity.dto.ActivityPageQuery;
import com.joinup.activity.dto.ActivityReviewRequest;
import com.joinup.activity.dto.ActivityUpdateRequest;
import com.joinup.activity.vo.ActivityDetailVO;
import com.joinup.activity.vo.ActivityPageVO;
import com.joinup.common.context.LoginUser;

public interface ActivityService {

    /** 创建活动。 */
    ActivityDetailVO createActivity(LoginUser loginUser, ActivityCreateRequest request);

    /** 编辑活动。 */
    ActivityDetailVO updateActivity(LoginUser loginUser, Long activityId, ActivityUpdateRequest request);

    /** 查询活动详情。 */
    ActivityDetailVO getActivityDetail(Long activityId);

    /** 分页查询活动。 */
    ActivityPageVO pageActivities(ActivityPageQuery query);

    /** 取消活动。 */
    ActivityDetailVO cancelActivity(LoginUser loginUser, Long activityId, String reason);

    /** 管理员审核活动。 */
    ActivityDetailVO reviewActivity(LoginUser loginUser, Long activityId, ActivityReviewRequest request);
}

package com.joinup.signup.service;

import com.joinup.common.context.LoginUser;
import com.joinup.signup.vo.SignupActivityResultVO;
import com.joinup.signup.vo.SignupApplyVO;
import com.joinup.signup.vo.SignupMyItemVO;

import java.util.List;

/**
 * 报名应用服务接口。
 * <p>
 * 该接口定义控制层可直接调用的报名模块核心用例，
 * 由实现类负责串联 Redis、Kafka、数据库查询等多个基础设施组件。
 * </p>
 */
public interface SignupService {

    /**
     * 申请参加指定活动。
     *
     * @param activityId 活动 ID
     * @param loginUser 当前登录用户
     * @return 报名受理结果
     */
    SignupApplyVO apply(Long activityId, LoginUser loginUser);

    /**
     * 提交取消报名请求。
     *
     * @param activityId 活动 ID
     * @param loginUser 当前登录用户
     * @param reason 取消原因，可为空
     * @return 取消受理结果
     */
    SignupApplyVO cancel(Long activityId, LoginUser loginUser, String reason);

    /**
     * 查询当前用户最近的报名记录。
     *
     * @param loginUser 当前登录用户
     * @return 报名记录列表
     */
    List<SignupMyItemVO> listMySignups(LoginUser loginUser);

    /**
     * 查询当前用户在指定活动上的报名结果。
     *
     * @param activityId 活动 ID
     * @param loginUser 当前登录用户
     * @return 当前活动的报名结果视图
     */
    SignupActivityResultVO getCurrentUserActivityResult(Long activityId, LoginUser loginUser);
}

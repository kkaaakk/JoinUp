package com.joinup.credit.service;

import com.joinup.common.context.LoginUser;
import com.joinup.credit.dto.AdminCreditAdjustRequest;
import com.joinup.credit.dto.CreditAttendanceRewardRequest;
import com.joinup.credit.dto.CreditCancelRequest;
import com.joinup.credit.dto.CreditNoShowPenaltyRequest;
import com.joinup.credit.dto.CreditSignupCheckRequest;
import com.joinup.credit.vo.UserCreditDetailVO;

/**
 * 信用模块应用服务。
 */
public interface CreditService {

    /**
     * 查询当前用户的信用详情。
     *
     * @param currentUserId 当前用户 ID
     * @return 信用详情
     */
    UserCreditDetailVO getCurrentUserCreditDetail(Long currentUserId);

    /**
     * 校验用户是否允许创建活动。
     *
     * @param userId 用户 ID
     */
    void assertCanCreateActivity(Long userId);

    /**
     * 校验用户是否允许报名某个活动。
     *
     * @param request 报名前信用校验请求
     */
    void assertCanSignup(CreditSignupCheckRequest request);

    /**
     * 判断用户是否需要被降低候补优先级。
     *
     * @param userId 用户 ID
     * @return 如果需要降低优先级则返回 {@code true}
     */
    boolean shouldDeprioritizeWaitlist(Long userId);

    /**
     * 记录一次守约加分。
     *
     * @param request 守约加分请求
     * @return 调整后的信用详情
     */
    UserCreditDetailVO recordAttendanceReward(CreditAttendanceRewardRequest request);

    /**
     * 记录一次取消行为对应的信用变更。
     *
     * @param request 取消处理请求
     * @return 调整后的信用详情
     */
    UserCreditDetailVO recordSignupCancel(CreditCancelRequest request);

    /**
     * 记录一次爽约扣分。
     *
     * @param request 爽约扣分请求
     * @return 调整后的信用详情
     */
    UserCreditDetailVO recordNoShowPenalty(CreditNoShowPenaltyRequest request);

    /**
     * 管理员人工调整用户信用分。
     *
     * @param operator 当前操作人
     * @param targetUserId 被调整用户 ID
     * @param request 调整请求
     * @return 调整后的信用详情
     */
    UserCreditDetailVO manualAdjust(LoginUser operator, Long targetUserId, AdminCreditAdjustRequest request);
}
package com.joinup.admin.service;

import com.joinup.admin.dto.AdminUserCreditAdjustRequest;
import com.joinup.admin.dto.AdminRiskUserPageQuery;
import com.joinup.admin.vo.AdminRiskUserPageVO;
import com.joinup.common.context.LoginUser;
import com.joinup.credit.vo.UserCreditDetailVO;
import jakarta.validation.Valid;

/**
 * 管理后台用户治理服务。
 */
public interface AdminUserService {

    /**
     * 管理员人工调整用户信用分。
     *
     * @param loginUser 当前登录管理员
     * @param request 调整请求
     * @return 调整后的信用详情
     */
    UserCreditDetailVO adjustUserCredit(LoginUser loginUser, @Valid AdminUserCreditAdjustRequest request);

    /**
     * 分页查询风险用户。
     *
     * @param loginUser 当前登录管理员
     * @param query 分页查询参数
     * @return 风险用户分页结果
     */
    AdminRiskUserPageVO pageRiskUsers(LoginUser loginUser, @Valid AdminRiskUserPageQuery query);
}
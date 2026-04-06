package com.joinup.admin.controller;

import com.joinup.admin.dto.AdminRiskUserPageQuery;
import com.joinup.admin.dto.AdminUserCreditAdjustRequest;
import com.joinup.admin.service.AdminUserService;
import com.joinup.admin.vo.AdminRiskUserPageVO;
import com.joinup.common.context.LoginUser;
import com.joinup.common.result.Result;
import com.joinup.credit.vo.UserCreditDetailVO;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台用户治理接口。
 */
@Validated
@RestController
@RequestMapping("/api/admin/user")
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * 创建管理后台用户治理控制器。
     *
     * @param adminUserService 用户治理服务
     */
    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /**
     * 管理员人工调整用户信用分。
     *
     * @param loginUser 当前登录管理员
     * @param request 调整请求
     * @return 调整后的信用详情
     */
    @PostMapping("/credit/adjust")
    public Result<UserCreditDetailVO> adjustUserCredit(@AuthenticationPrincipal LoginUser loginUser,
                                                       @Valid @RequestBody AdminUserCreditAdjustRequest request) {
        return Result.success(adminUserService.adjustUserCredit(loginUser, request));
    }

    /**
     * 分页查询风险用户列表。
     *
     * @param loginUser 当前登录管理员
     * @param query 查询参数
     * @return 风险用户分页结果
     */
    @GetMapping("/risk/page")
    public Result<AdminRiskUserPageVO> pageRiskUsers(@AuthenticationPrincipal LoginUser loginUser,
                                                     @Valid AdminRiskUserPageQuery query) {
        return Result.success(adminUserService.pageRiskUsers(loginUser, query));
    }
}
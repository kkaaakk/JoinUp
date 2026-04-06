package com.joinup.credit.controller;

import com.joinup.common.context.LoginUser;
import com.joinup.common.result.Result;
import com.joinup.credit.dto.AdminCreditAdjustRequest;
import com.joinup.credit.service.CreditService;
import com.joinup.credit.vo.UserCreditDetailVO;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 信用管理后台接口。
 */
@Validated
@RestController
@RequestMapping("/api/admin/credit")
public class AdminCreditController {

    private final CreditService creditService;

    /**
     * 创建信用管理后台控制器。
     *
     * @param creditService 信用模块应用服务
     */
    public AdminCreditController(CreditService creditService) {
        this.creditService = creditService;
    }

    /**
     * 管理员人工调整指定用户的信用分。
     *
     * @param loginUser 当前登录用户快照
     * @param userId 被调整用户 ID
     * @param request 调整请求
     * @return 调整后的信用详情
     */
    @PostMapping("/adjust/{userId}")
    public Result<UserCreditDetailVO> adjustCredit(@AuthenticationPrincipal LoginUser loginUser,
                                                   @PathVariable("userId") Long userId,
                                                   @Valid @RequestBody AdminCreditAdjustRequest request) {
        return Result.success(creditService.manualAdjust(loginUser, userId, request));
    }
}
package com.joinup.credit.controller;

import com.joinup.common.context.LoginUser;
import com.joinup.common.exception.BusinessException;
import com.joinup.common.exception.ErrorCode;
import com.joinup.common.result.Result;
import com.joinup.credit.service.CreditService;
import com.joinup.credit.vo.UserCreditDetailVO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户信用查询接口。
 */
@Validated
@RestController
@RequestMapping("/api/credit")
public class CreditController {

    private final CreditService creditService;

    /**
     * 创建信用查询控制器。
     *
     * @param creditService 信用模块应用服务
     */
    public CreditController(CreditService creditService) {
        this.creditService = creditService;
    }

    /**
     * 查询当前登录用户的信用详情。
     *
     * @param loginUser 当前登录用户快照
     * @return 信用详情
     */
    @GetMapping("/my")
    public Result<UserCreditDetailVO> getMyCredit(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(creditService.getCurrentUserCreditDetail(currentUserId(loginUser)));
    }

    /**
     * 从认证上下文中提取当前用户 ID。
     *
     * @param loginUser 当前登录用户快照
     * @return 当前用户 ID
     */
    private Long currentUserId(LoginUser loginUser) {
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return loginUser.getUserId();
    }
}
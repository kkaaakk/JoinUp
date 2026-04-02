package com.joinup.credit.domain;

import com.joinup.common.context.LoginUser;
import com.joinup.common.exception.BusinessException;
import com.joinup.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 信用模块权限校验器。
 * <p>
 * 当前阶段先保留一个轻量的管理员校验入口，后续接入统一 RBAC 时只需要替换这里的实现。
 * </p>
 */
@Component
public class CreditPermissionChecker {

    private static final String TEMP_ADMIN_USERNAME = "admin";

    /**
     * 校验当前用户是否已登录。
     *
     * @param loginUser 当前登录用户快照
     */
    public void assertLogin(LoginUser loginUser) {
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    /**
     * 校验当前用户是否具备信用管理权限。
     *
     * @param loginUser 当前登录用户快照
     */
    public void assertAdminOperator(LoginUser loginUser) {
        assertLogin(loginUser);
        if (!Objects.equals(TEMP_ADMIN_USERNAME, loginUser.getUsername())) {
            throw new BusinessException(ErrorCode.CREDIT_PERMISSION_DENIED, "当前用户不具备信用管理权限");
        }
    }
}
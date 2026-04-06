package com.joinup.activity.domain;

import com.joinup.activity.entity.ActivityEntity;
import com.joinup.common.context.LoginUser;
import com.joinup.common.exception.BusinessException;
import com.joinup.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 活动模块权限校验器。
 * 把“是否登录、是否发起者、是否管理员”从 service 中抽出来，便于后续替换成 RBAC。
 */
@Component
public class ActivityPermissionChecker {

    private static final String TEMP_ADMIN_USERNAME = "admin";

    public void assertLogin(LoginUser loginUser) {
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    public void assertOrganizer(LoginUser loginUser, ActivityEntity activity) {
        assertLogin(loginUser);
        if (!Objects.equals(loginUser.getUserId(), activity.getOrganizerId())) {
            throw new BusinessException(ErrorCode.ACTIVITY_PERMISSION_DENIED, "Only organizer can operate this activity");
        }
    }

    public void assertAdminReviewer(LoginUser loginUser) {
        assertLogin(loginUser);
        // Reserved hook for future RBAC integration.
        if (!Objects.equals(TEMP_ADMIN_USERNAME, loginUser.getUsername())) {
            throw new BusinessException(ErrorCode.ACTIVITY_REVIEW_PERMISSION_DENIED,
                    "Admin review permission is not granted for current user");
        }
    }
}

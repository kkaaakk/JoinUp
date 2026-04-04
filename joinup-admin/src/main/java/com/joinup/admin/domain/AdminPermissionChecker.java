package com.joinup.admin.domain;

import com.joinup.common.context.LoginUser;
import com.joinup.common.exception.BusinessException;
import com.joinup.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 管理后台权限校验器。
 * <p>
 * 当前阶段项目还没有完整的 RBAC 模型，因此这里先保留一个非常轻量的兜底实现：
 * 只允许用户名为 {@code admin} 的用户进入管理后台接口。
 * 后续如果接入真正的角色、菜单和按钮权限，只需要替换这里的实现即可，
 * 上层 Controller 和 Service 的调用方式不需要变化。
 * </p>
 */
@Component
public class AdminPermissionChecker {

    /**
     * 临时管理员用户名。
     * <p>
     * 这是现阶段的占位方案，真正接入 RBAC 后应当由角色/权限系统替代。
     * </p>
     */
    private static final String TEMP_ADMIN_USERNAME = "admin";

    /**
     * 校验当前用户是否已经登录。
     * <p>
     * 管理后台所有接口都要求先完成身份认证，因此这里先统一兜住“未登录”场景。
     * </p>
     *
     * @param loginUser 当前登录用户快照
     */
    public void assertLogin(LoginUser loginUser) {
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    /**
     * 校验当前用户是否具备管理员权限。
     * <p>
     * 这里额外保留了 {@code permissionCode} 参数，目的是让调用方在现阶段就按 RBAC 思维书写代码。
     * 这样后续真正接入细粒度权限时，我们只需要在这里把权限编码接到鉴权系统即可，
     * 上层业务代码不需要重新改签名。
     * </p>
     *
     * @param loginUser 当前登录用户快照
     * @param permissionCode 目标操作对应的权限编码
     */
    public void assertAdmin(LoginUser loginUser, String permissionCode) {
        assertLogin(loginUser);
        if (!Objects.equals(TEMP_ADMIN_USERNAME, loginUser.getUsername())) {
            throw new BusinessException(ErrorCode.ADMIN_PERMISSION_DENIED,
                    "当前用户不具备管理后台权限，无法执行操作：" + permissionCode);
        }
    }
}
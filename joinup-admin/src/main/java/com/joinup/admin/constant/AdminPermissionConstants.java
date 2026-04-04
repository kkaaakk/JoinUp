package com.joinup.admin.constant;

/**
 * 管理后台权限编码常量。
 * <p>
 * 当前项目还没有正式接入 RBAC，因此这些常量暂时主要承担“权限编码约定”的作用，
 * 方便后续把 Spring Security、菜单、按钮权限、审计日志统一收敛到同一套编码体系。
 * </p>
 */
public final class AdminPermissionConstants {

    /** 活动审核权限。 */
    public static final String ACTIVITY_REVIEW = "admin:activity:review";
    /** 举报处理权限。 */
    public static final String REPORT_HANDLE = "admin:report:handle";
    /** 用户信用人工调整权限。 */
    public static final String USER_CREDIT_ADJUST = "admin:user:credit:adjust";
    /** 热门活动监控查看权限。 */
    public static final String HOT_ACTIVITY_VIEW = "admin:monitor:hot:view";
    /** 风险用户监控查看权限。 */
    public static final String RISK_USER_VIEW = "admin:monitor:risk:view";
    /** 操作日志查看权限。 */
    public static final String LOG_VIEW = "admin:log:view";

    /**
     * 私有构造方法。
     * <p>
     * 常量类只负责提供静态字段，不允许创建实例。
     * </p>
     */
    private AdminPermissionConstants() {
    }
}
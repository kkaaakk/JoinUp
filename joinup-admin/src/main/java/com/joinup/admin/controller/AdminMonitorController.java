package com.joinup.admin.controller;

import com.joinup.admin.dto.AdminHotActivityPageQuery;
import com.joinup.admin.service.AdminMonitorService;
import com.joinup.admin.vo.AdminHotActivityPageVO;
import com.joinup.common.context.LoginUser;
import com.joinup.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台监控接口。
 */
@Validated
@RestController
@RequestMapping("/api/admin/activity")
public class AdminMonitorController {

    private final AdminMonitorService adminMonitorService;

    /**
     * 创建管理后台监控控制器。
     *
     * @param adminMonitorService 管理后台监控服务
     */
    public AdminMonitorController(AdminMonitorService adminMonitorService) {
        this.adminMonitorService = adminMonitorService;
    }

    /**
     * 分页查询热门活动监控列表。
     *
     * @param loginUser 当前登录管理员
     * @param query 查询参数
     * @return 热门活动分页结果
     */
    @GetMapping("/hot/page")
    public Result<AdminHotActivityPageVO> pageHotActivities(@AuthenticationPrincipal LoginUser loginUser,
                                                            @Valid AdminHotActivityPageQuery query) {
        return Result.success(adminMonitorService.pageHotActivities(loginUser, query));
    }
}
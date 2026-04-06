package com.joinup.admin.controller;

import com.joinup.admin.dto.AdminReportHandleRequest;
import com.joinup.admin.dto.AdminReportPageQuery;
import com.joinup.admin.service.AdminReportService;
import com.joinup.admin.vo.AdminReportItemVO;
import com.joinup.admin.vo.AdminReportPageVO;
import com.joinup.common.context.LoginUser;
import com.joinup.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台举报处理接口。
 */
@Validated
@RestController
@RequestMapping("/api/admin/report")
public class AdminReportController {

    private final AdminReportService adminReportService;

    /**
     * 创建管理后台举报控制器。
     *
     * @param adminReportService 举报处理服务
     */
    public AdminReportController(AdminReportService adminReportService) {
        this.adminReportService = adminReportService;
    }

    /**
     * 分页查询举报列表。
     *
     * @param loginUser 当前登录管理员
     * @param query 查询参数
     * @return 举报分页结果
     */
    @GetMapping("/page")
    public Result<AdminReportPageVO> pageReports(@AuthenticationPrincipal LoginUser loginUser,
                                                 @Valid AdminReportPageQuery query) {
        return Result.success(adminReportService.pageReports(loginUser, query));
    }

    /**
     * 处理指定举报。
     *
     * @param loginUser 当前登录管理员
     * @param id 举报记录 ID
     * @param request 处理请求
     * @return 处理后的举报详情
     */
    @PostMapping("/handle/{id}")
    public Result<AdminReportItemVO> handleReport(@AuthenticationPrincipal LoginUser loginUser,
                                                  @PathVariable("id") Long id,
                                                  @Valid @RequestBody AdminReportHandleRequest request) {
        return Result.success(adminReportService.handleReport(loginUser, id, request));
    }
}
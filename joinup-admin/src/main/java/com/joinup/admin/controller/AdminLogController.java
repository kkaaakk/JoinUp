package com.joinup.admin.controller;

import com.joinup.admin.dto.OperationLogPageQuery;
import com.joinup.admin.service.OperationLogService;
import com.joinup.admin.vo.OperationLogPageVO;
import com.joinup.common.context.LoginUser;
import com.joinup.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台操作日志接口。
 */
@Validated
@RestController
@RequestMapping("/api/admin/log")
public class AdminLogController {

    private final OperationLogService operationLogService;

    /**
     * 创建管理后台日志控制器。
     *
     * @param operationLogService 操作日志服务
     */
    public AdminLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    /**
     * 分页查询后台操作日志。
     *
     * @param loginUser 当前登录管理员
     * @param query 查询参数
     * @return 操作日志分页结果
     */
    @GetMapping("/page")
    public Result<OperationLogPageVO> pageLogs(@AuthenticationPrincipal LoginUser loginUser,
                                               @Valid OperationLogPageQuery query) {
        return Result.success(operationLogService.pageOperationLogs(loginUser, query));
    }
}
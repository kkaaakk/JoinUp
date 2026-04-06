package com.joinup.admin.service;

import com.joinup.admin.dto.AdminReportHandleRequest;
import com.joinup.admin.dto.AdminReportPageQuery;
import com.joinup.admin.vo.AdminReportItemVO;
import com.joinup.admin.vo.AdminReportPageVO;
import com.joinup.common.context.LoginUser;
import jakarta.validation.Valid;

/**
 * 管理后台举报处理服务。
 */
public interface AdminReportService {

    /**
     * 分页查询举报列表。
     *
     * @param loginUser 当前登录管理员
     * @param query 分页查询参数
     * @return 举报分页结果
     */
    AdminReportPageVO pageReports(LoginUser loginUser, @Valid AdminReportPageQuery query);

    /**
     * 处理指定举报。
     *
     * @param loginUser 当前登录管理员
     * @param reportId 举报记录 ID
     * @param request 处理请求
     * @return 处理后的举报详情
     */
    AdminReportItemVO handleReport(LoginUser loginUser, Long reportId, @Valid AdminReportHandleRequest request);
}
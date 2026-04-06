package com.joinup.admin.service;

import com.joinup.admin.dto.AdminHotActivityPageQuery;
import com.joinup.admin.vo.AdminHotActivityPageVO;
import com.joinup.common.context.LoginUser;
import jakarta.validation.Valid;

/**
 * 管理后台监控服务。
 */
public interface AdminMonitorService {

    /**
     * 分页查询热门活动监控列表。
     *
     * @param loginUser 当前登录管理员
     * @param query 查询参数
     * @return 热门活动分页结果
     */
    AdminHotActivityPageVO pageHotActivities(LoginUser loginUser, @Valid AdminHotActivityPageQuery query);
}
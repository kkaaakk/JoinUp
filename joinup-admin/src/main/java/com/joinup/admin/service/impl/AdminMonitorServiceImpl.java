package com.joinup.admin.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.joinup.activity.entity.ActivityEntity;
import com.joinup.activity.enums.ActivityStatusEnum;
import com.joinup.admin.constant.AdminPermissionConstants;
import com.joinup.admin.domain.AdminPermissionChecker;
import com.joinup.admin.dto.AdminHotActivityPageQuery;
import com.joinup.admin.mapper.AdminMonitorMapper;
import com.joinup.admin.service.AdminMonitorService;
import com.joinup.admin.vo.AdminHotActivityItemVO;
import com.joinup.admin.vo.AdminHotActivityPageVO;
import com.joinup.common.context.LoginUser;
import com.joinup.user.entity.UserEntity;
import com.joinup.user.mapper.UserMapper;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * 管理后台监控服务实现。
 */
@Service
@Validated
public class AdminMonitorServiceImpl implements AdminMonitorService {

    private final AdminMonitorMapper adminMonitorMapper;
    private final UserMapper userMapper;
    private final AdminPermissionChecker adminPermissionChecker;

    /**
     * 创建管理后台监控服务实现。
     *
     * @param adminMonitorMapper 管理后台监控 Mapper
     * @param userMapper 用户 Mapper
     * @param adminPermissionChecker 管理员权限校验器
     */
    public AdminMonitorServiceImpl(AdminMonitorMapper adminMonitorMapper,
                                   UserMapper userMapper,
                                   AdminPermissionChecker adminPermissionChecker) {
        this.adminMonitorMapper = adminMonitorMapper;
        this.userMapper = userMapper;
        this.adminPermissionChecker = adminPermissionChecker;
    }

    /**
     * 分页查询热门活动监控列表。
     * <p>
     * 这个接口面向运营与风控后台，用来快速观察当前平台上热度较高、可能需要重点关注的活动。
     * 当前默认按热度分和浏览量排序，后续可以很自然地扩展成更多监控维度。
     * </p>
     *
     * @param loginUser 当前登录管理员
     * @param query 查询参数
     * @return 热门活动分页结果
     */
    @Override
    public AdminHotActivityPageVO pageHotActivities(LoginUser loginUser, @Valid AdminHotActivityPageQuery query) {
        adminPermissionChecker.assertAdmin(loginUser, AdminPermissionConstants.HOT_ACTIVITY_VIEW);

        Page<ActivityEntity> page = adminMonitorMapper.selectHotActivityPage(
                new Page<>(query.getCurrent(), query.getSize()),
                query.getMinHeatScore());

        List<AdminHotActivityItemVO> records = page.getRecords().stream()
                .map(this::mapHotActivity)
                .toList();

        return AdminHotActivityPageVO.builder()
                .current(page.getCurrent())
                .size(page.getSize())
                .total(page.getTotal())
                .records(records)
                .build();
    }

    /**
     * 把活动实体转换成热门活动监控视图。
     *
     * @param entity 活动实体
     * @return 热门活动监控视图
     */
    private AdminHotActivityItemVO mapHotActivity(ActivityEntity entity) {
        UserEntity organizer = entity.getOrganizerId() != null ? userMapper.selectById(entity.getOrganizerId()) : null;
        return AdminHotActivityItemVO.builder()
                .activityId(entity.getId())
                .title(entity.getTitle())
                .category(entity.getCategory())
                .organizerId(entity.getOrganizerId())
                .organizerUsername(organizer != null ? organizer.getUsername() : null)
                .status(entity.getStatus())
                .statusDescription(resolveActivityStatusDescription(entity.getStatus()))
                .heatScore(entity.getHeatScore())
                .viewCount(entity.getViewCount())
                .currentParticipants(entity.getCurrentParticipants())
                .maxParticipants(entity.getMaxParticipants())
                .signupDeadline(entity.getSignupDeadline())
                .build();
    }

    /**
     * 把活动状态编码转换成中文描述。
     *
     * @param status 活动状态编码
     * @return 中文描述
     */
    private String resolveActivityStatusDescription(Integer status) {
        if (status == null) {
            return null;
        }
        for (ActivityStatusEnum value : ActivityStatusEnum.values()) {
            if (value.getCode() == status) {
                return value.getDescription();
            }
        }
        return "未知状态";
    }
}
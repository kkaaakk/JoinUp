package com.joinup.admin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.joinup.activity.entity.ActivityEntity;
import com.joinup.activity.entity.ActivityReportEntity;
import com.joinup.activity.enums.ActivityStatusEnum;
import com.joinup.activity.enums.ReportStatusEnum;
import com.joinup.activity.enums.ReportTypeEnum;
import com.joinup.activity.mapper.ActivityMapper;
import com.joinup.admin.constant.AdminPermissionConstants;
import com.joinup.admin.domain.AdminPermissionChecker;
import com.joinup.admin.dto.AdminReportHandleRequest;
import com.joinup.admin.dto.AdminReportPageQuery;
import com.joinup.admin.enums.OperationResultEnum;
import com.joinup.admin.mapper.AdminActivityReportMapper;
import com.joinup.admin.service.AdminReportService;
import com.joinup.admin.service.OperationLogService;
import com.joinup.admin.vo.AdminReportItemVO;
import com.joinup.admin.vo.AdminReportPageVO;
import com.joinup.common.context.LoginUser;
import com.joinup.common.exception.BusinessException;
import com.joinup.common.exception.ErrorCode;
import com.joinup.user.entity.UserEntity;
import com.joinup.user.enums.UserStatusEnum;
import com.joinup.user.mapper.UserMapper;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 管理后台举报处理服务实现。
 * <p>
 * 这层把举报处理真正会影响业务状态的动作收在一起：
 * 举报本身状态更新、异常活动下架、被举报用户禁用，以及后台操作日志落库。
 * 这样可以保证后台治理动作的语义集中且便于后续继续增强。
 * </p>
 */
@Service
@Validated
public class AdminReportServiceImpl implements AdminReportService {

    private final AdminActivityReportMapper adminActivityReportMapper;
    private final ActivityMapper activityMapper;
    private final UserMapper userMapper;
    private final AdminPermissionChecker adminPermissionChecker;
    private final OperationLogService operationLogService;

    /**
     * 创建管理后台举报处理服务。
     *
     * @param adminActivityReportMapper 举报 Mapper
     * @param activityMapper 活动 Mapper
     * @param userMapper 用户 Mapper
     * @param adminPermissionChecker 管理员权限校验器
     * @param operationLogService 操作日志服务
     */
    public AdminReportServiceImpl(AdminActivityReportMapper adminActivityReportMapper,
                                  ActivityMapper activityMapper,
                                  UserMapper userMapper,
                                  AdminPermissionChecker adminPermissionChecker,
                                  OperationLogService operationLogService) {
        this.adminActivityReportMapper = adminActivityReportMapper;
        this.activityMapper = activityMapper;
        this.userMapper = userMapper;
        this.adminPermissionChecker = adminPermissionChecker;
        this.operationLogService = operationLogService;
    }

    /**
     * 分页查询举报列表。
     * <p>
     * 这里以后台治理视角提供多维筛选，方便管理员按状态、活动、举报人、被举报人快速定位问题。
     * </p>
     *
     * @param loginUser 当前登录管理员
     * @param query 分页查询参数
     * @return 举报分页结果
     */
    @Override
    public AdminReportPageVO pageReports(LoginUser loginUser, @Valid AdminReportPageQuery query) {
        adminPermissionChecker.assertAdmin(loginUser, AdminPermissionConstants.REPORT_HANDLE);

        Page<ActivityReportEntity> page = adminActivityReportMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()),
                Wrappers.<ActivityReportEntity>lambdaQuery()
                        .eq(query.getStatus() != null, ActivityReportEntity::getStatus, query.getStatus())
                        .eq(query.getReportType() != null, ActivityReportEntity::getReportType, query.getReportType())
                        .eq(query.getActivityId() != null, ActivityReportEntity::getActivityId, query.getActivityId())
                        .eq(query.getReporterUserId() != null, ActivityReportEntity::getReporterUserId, query.getReporterUserId())
                        .eq(query.getReportedUserId() != null, ActivityReportEntity::getReportedUserId, query.getReportedUserId())
                        .orderByDesc(ActivityReportEntity::getCreatedAt)
                        .orderByDesc(ActivityReportEntity::getId));

        List<AdminReportItemVO> records = page.getRecords().stream()
                .map(this::mapReportItem)
                .toList();

        return AdminReportPageVO.builder()
                .current(page.getCurrent())
                .size(page.getSize())
                .total(page.getTotal())
                .records(records)
                .build();
    }

    /**
     * 处理指定举报。
     * <p>
     * 当前支持的治理动作包括：
     * 1. 更新举报状态；
     * 2. 需要时下架被举报活动；
     * 3. 需要时禁用被举报用户；
     * 4. 统一写入后台操作日志。
     * </p>
     *
     * @param loginUser 当前登录管理员
     * @param reportId 举报记录 ID
     * @param request 处理请求
     * @return 处理后的举报详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminReportItemVO handleReport(LoginUser loginUser, Long reportId, @Valid AdminReportHandleRequest request) {
        adminPermissionChecker.assertAdmin(loginUser, AdminPermissionConstants.REPORT_HANDLE);

        ActivityReportEntity report = loadReportOrThrow(reportId);
        ReportStatusEnum targetStatus = resolveFinalReportStatus(request.getTargetStatus());
        assertHandleAllowed(report);

        report.setStatus(targetStatus.getCode());
        report.setHandlerUserId(loginUser.getUserId());
        report.setHandleResult(request.getHandleResult().trim());
        report.setHandledAt(LocalDateTime.now());
        adminActivityReportMapper.updateById(report);

        if (Objects.equals(targetStatus, ReportStatusEnum.APPROVED)) {
            offShelfActivityIfNecessary(report, request);
            disableReportedUserIfNecessary(report, request);
        }

        operationLogService.recordOperation(loginUser,
                "report_admin",
                "HANDLE",
                "report",
                report.getId(),
                OperationResultEnum.SUCCESS,
                null,
                buildHandleExtraData(request));

        return mapReportItem(loadReportOrThrow(reportId));
    }

    /**
     * 根据举报 ID 加载举报记录。
     *
     * @param reportId 举报记录 ID
     * @return 举报实体
     */
    private ActivityReportEntity loadReportOrThrow(Long reportId) {
        ActivityReportEntity report = adminActivityReportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(ErrorCode.ADMIN_REPORT_NOT_FOUND, "举报记录不存在，reportId=" + reportId);
        }
        return report;
    }

    /**
     * 校验举报当前是否允许被处理。
     * <p>
     * 已经终态的举报不允许重复处理，避免后台多次点击造成状态漂移。
     * </p>
     *
     * @param report 举报实体
     */
    private void assertHandleAllowed(ActivityReportEntity report) {
        if (Objects.equals(report.getStatus(), ReportStatusEnum.APPROVED.getCode())
                || Objects.equals(report.getStatus(), ReportStatusEnum.REJECTED.getCode())) {
            throw new BusinessException(ErrorCode.ADMIN_REPORT_STATUS_CONFLICT,
                    "当前举报已经完成处理，不能重复操作");
        }
    }

    /**
     * 解析并校验最终举报状态。
     * <p>
     * 管理后台处理举报时，只允许把状态推进到终态：通过或驳回。
     * 不允许在处理接口里写入其他中间态，避免后台治理流程混乱。
     * </p>
     *
     * @param targetStatus 目标状态编码
     * @return 终态枚举
     */
    private ReportStatusEnum resolveFinalReportStatus(Integer targetStatus) {
        if (Objects.equals(targetStatus, ReportStatusEnum.APPROVED.getCode())) {
            return ReportStatusEnum.APPROVED;
        }
        if (Objects.equals(targetStatus, ReportStatusEnum.REJECTED.getCode())) {
            return ReportStatusEnum.REJECTED;
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "举报处理结果只允许设置为 APPROVED 或 REJECTED");
    }

    /**
     * 视情况执行活动下架。
     * <p>
     * 只有当举报被判定为成立且管理员明确勾选“下架活动”时，才会推进活动状态到取消态。
     * </p>
     *
     * @param report 举报实体
     * @param request 处理请求
     */
    private void offShelfActivityIfNecessary(ActivityReportEntity report, AdminReportHandleRequest request) {
        if (!Boolean.TRUE.equals(request.getOffShelfActivity()) || report.getActivityId() == null) {
            return;
        }

        ActivityEntity activity = activityMapper.selectById(report.getActivityId());
        if (activity == null) {
            return;
        }
        if (Objects.equals(activity.getStatus(), ActivityStatusEnum.CANCELED.getCode())
                || Objects.equals(activity.getStatus(), ActivityStatusEnum.FINISHED.getCode())) {
            return;
        }

        activity.setStatus(ActivityStatusEnum.CANCELED.getCode());
        activity.setCancelReason("管理员下架：" + request.getHandleResult().trim());
        activityMapper.updateById(activity);
    }

    /**
     * 视情况禁用被举报用户。
     *
     * @param report 举报实体
     * @param request 处理请求
     */
    private void disableReportedUserIfNecessary(ActivityReportEntity report, AdminReportHandleRequest request) {
        if (!Boolean.TRUE.equals(request.getDisableReportedUser()) || report.getReportedUserId() == null) {
            return;
        }

        UserEntity user = userMapper.selectById(report.getReportedUserId());
        if (user == null) {
            return;
        }
        if (Objects.equals(user.getStatus(), UserStatusEnum.DISABLED.getCode())) {
            return;
        }

        user.setStatus(UserStatusEnum.DISABLED.getCode());
        userMapper.updateById(user);
    }

    /**
     * 把举报实体转换成展示对象。
     *
     * @param entity 举报实体
     * @return 举报展示对象
     */
    private AdminReportItemVO mapReportItem(ActivityReportEntity entity) {
        ActivityEntity activity = entity.getActivityId() != null ? activityMapper.selectById(entity.getActivityId()) : null;
        UserEntity reporter = entity.getReporterUserId() != null ? userMapper.selectById(entity.getReporterUserId()) : null;
        UserEntity reportedUser = entity.getReportedUserId() != null ? userMapper.selectById(entity.getReportedUserId()) : null;
        UserEntity handler = entity.getHandlerUserId() != null ? userMapper.selectById(entity.getHandlerUserId()) : null;

        return AdminReportItemVO.builder()
                .id(entity.getId())
                .activityId(entity.getActivityId())
                .activityTitle(activity != null ? activity.getTitle() : null)
                .reporterUserId(entity.getReporterUserId())
                .reporterUsername(reporter != null ? reporter.getUsername() : null)
                .reportedUserId(entity.getReportedUserId())
                .reportedUsername(reportedUser != null ? reportedUser.getUsername() : null)
                .reportType(entity.getReportType())
                .reportTypeDescription(resolveReportTypeDescription(entity.getReportType()))
                .reason(entity.getReason())
                .evidenceUrls(entity.getEvidenceUrls())
                .status(entity.getStatus())
                .statusDescription(resolveReportStatusDescription(entity.getStatus()))
                .handlerUserId(entity.getHandlerUserId())
                .handlerUsername(handler != null ? handler.getUsername() : null)
                .handleResult(entity.getHandleResult())
                .handledAt(entity.getHandledAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * 生成处理动作的扩展日志内容。
     *
     * @param request 处理请求
     * @return 可直接写入操作日志的摘要字符串
     */
    private String buildHandleExtraData(AdminReportHandleRequest request) {
        return "targetStatus=" + request.getTargetStatus()
                + ", offShelfActivity=" + request.getOffShelfActivity()
                + ", disableReportedUser=" + request.getDisableReportedUser()
                + ", handleResult=" + request.getHandleResult().trim();
    }

    /**
     * 把举报状态编码转换成中文描述。
     *
     * @param status 举报状态编码
     * @return 中文描述
     */
    private String resolveReportStatusDescription(Integer status) {
        if (status == null) {
            return null;
        }
        for (ReportStatusEnum value : ReportStatusEnum.values()) {
            if (value.getCode() == status) {
                return value.getDescription();
            }
        }
        return "未知状态";
    }

    /**
     * 把举报类型编码转换成中文描述。
     *
     * @param reportType 举报类型编码
     * @return 中文描述
     */
    private String resolveReportTypeDescription(Integer reportType) {
        if (reportType == null) {
            return null;
        }
        for (ReportTypeEnum value : ReportTypeEnum.values()) {
            if (value.getCode() == reportType) {
                return value.getDescription();
            }
        }
        return "未知类型";
    }
}
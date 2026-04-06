package com.joinup.admin.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.joinup.admin.constant.AdminPermissionConstants;
import com.joinup.admin.domain.AdminPermissionChecker;
import com.joinup.admin.dto.AdminRiskUserPageQuery;
import com.joinup.admin.dto.AdminUserCreditAdjustRequest;
import com.joinup.admin.enums.OperationResultEnum;
import com.joinup.admin.mapper.AdminMonitorMapper;
import com.joinup.admin.service.AdminUserService;
import com.joinup.admin.service.OperationLogService;
import com.joinup.admin.vo.AdminRiskUserItemVO;
import com.joinup.admin.vo.AdminRiskUserPageVO;
import com.joinup.common.context.LoginUser;
import com.joinup.credit.domain.CreditRuleProperties;
import com.joinup.credit.dto.AdminCreditAdjustRequest;
import com.joinup.credit.service.CreditService;
import com.joinup.credit.vo.UserCreditDetailVO;
import com.joinup.user.entity.UserEntity;
import com.joinup.user.enums.UserStatusEnum;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * 管理后台用户治理服务实现。
 * <p>
 * 这个服务主要负责两件事：
 * 1. 统一承接后台用户信用人工干预；
 * 2. 面向运营与风控场景输出风险用户监控结果。
 * </p>
 */
@Service
@Validated
public class AdminUserServiceImpl implements AdminUserService {

    private final CreditService creditService;
    private final AdminMonitorMapper adminMonitorMapper;
    private final CreditRuleProperties creditRuleProperties;
    private final AdminPermissionChecker adminPermissionChecker;
    private final OperationLogService operationLogService;

    /**
     * 创建管理后台用户治理服务。
     *
     * @param creditService 信用模块服务
     * @param adminMonitorMapper 管理后台监控 Mapper
     * @param creditRuleProperties 信用规则配置
     * @param adminPermissionChecker 管理员权限校验器
     * @param operationLogService 操作日志服务
     */
    public AdminUserServiceImpl(CreditService creditService,
                                AdminMonitorMapper adminMonitorMapper,
                                CreditRuleProperties creditRuleProperties,
                                AdminPermissionChecker adminPermissionChecker,
                                OperationLogService operationLogService) {
        this.creditService = creditService;
        this.adminMonitorMapper = adminMonitorMapper;
        this.creditRuleProperties = creditRuleProperties;
        this.adminPermissionChecker = adminPermissionChecker;
        this.operationLogService = operationLogService;
    }

    /**
     * 管理员人工调整用户信用分。
     * <p>
     * joinup-admin 这里提供的是统一后台入口，真正的信用更新规则、分值边界和记录落库，
     * 仍然由 joinup-credit 模块负责，这样可以保证信用规则只有一套权威实现。
     * </p>
     *
     * @param loginUser 当前登录管理员
     * @param request 调整请求
     * @return 调整后的信用详情
     */
    @Override
    public UserCreditDetailVO adjustUserCredit(LoginUser loginUser, @Valid AdminUserCreditAdjustRequest request) {
        adminPermissionChecker.assertAdmin(loginUser, AdminPermissionConstants.USER_CREDIT_ADJUST);

        AdminCreditAdjustRequest delegateRequest = new AdminCreditAdjustRequest();
        delegateRequest.setDeltaScore(request.getDeltaScore());
        delegateRequest.setReason(request.getReason());

        UserCreditDetailVO result = creditService.manualAdjust(loginUser, request.getUserId(), delegateRequest);
        operationLogService.recordOperation(loginUser,
                "credit_admin",
                "ADJUST",
                "user_credit",
                request.getUserId(),
                OperationResultEnum.SUCCESS,
                null,
                "deltaScore=" + request.getDeltaScore() + ", reason=" + request.getReason());
        return result;
    }

    /**
     * 分页查询风险用户。
     * <p>
     * 这里默认把“低于风险阈值的用户”和“状态异常用户”都纳入结果，
     * 方便后台在同一个页面里快速发现需要人工关注的对象。
     * </p>
     *
     * @param loginUser 当前登录管理员
     * @param query 分页查询参数
     * @return 风险用户分页结果
     */
    @Override
    public AdminRiskUserPageVO pageRiskUsers(LoginUser loginUser, @Valid AdminRiskUserPageQuery query) {
        adminPermissionChecker.assertAdmin(loginUser, AdminPermissionConstants.RISK_USER_VIEW);

        Integer maxCreditScore = query.getMaxCreditScore() != null
                ? query.getMaxCreditScore()
                : creditRuleProperties.getHotActivitySignupMinScore();

        Page<UserEntity> page = adminMonitorMapper.selectRiskUserPage(
                new Page<>(query.getCurrent(), query.getSize()),
                maxCreditScore,
                query.getIncludeDisabled());

        List<AdminRiskUserItemVO> records = page.getRecords().stream()
                .map(user -> mapRiskUser(user, maxCreditScore))
                .toList();

        return AdminRiskUserPageVO.builder()
                .current(page.getCurrent())
                .size(page.getSize())
                .total(page.getTotal())
                .records(records)
                .build();
    }

    /**
     * 把用户实体转换成风险用户展示对象。
     *
     * @param user 用户实体
     * @param riskScoreThreshold 风险阈值
     * @return 风险用户视图对象
     */
    private AdminRiskUserItemVO mapRiskUser(UserEntity user, Integer riskScoreThreshold) {
        return AdminRiskUserItemVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .phone(user.getPhone())
                .email(user.getEmail())
                .status(user.getStatus())
                .statusDescription(resolveUserStatusDescription(user.getStatus()))
                .creditScore(user.getCreditScore())
                .riskReason(buildRiskReason(user, riskScoreThreshold))
                .lastLoginAt(user.getLastLoginAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    /**
     * 生成风险原因说明。
     * <p>
     * 这里把“低信用分”和“状态异常”组合成一段可直接展示给运营后台的文案，
     * 方便后台同学无需再根据编码手工判断风险来源。
     * </p>
     *
     * @param user 用户实体
     * @param riskScoreThreshold 风险阈值
     * @return 风险原因说明
     */
    private String buildRiskReason(UserEntity user, Integer riskScoreThreshold) {
        boolean lowCredit = user.getCreditScore() != null && user.getCreditScore() <= riskScoreThreshold;
        boolean abnormalStatus = user.getStatus() != null && user.getStatus() != UserStatusEnum.ACTIVE.getCode();

        if (lowCredit && abnormalStatus) {
            return "信用分偏低且用户状态异常";
        }
        if (lowCredit) {
            return "信用分低于风险阈值";
        }
        if (abnormalStatus) {
            return "用户状态异常，需要重点关注";
        }
        return "命中后台监控规则";
    }

    /**
     * 把用户状态编码转换成中文描述。
     *
     * @param status 用户状态编码
     * @return 中文描述
     */
    private String resolveUserStatusDescription(Integer status) {
        if (status == null) {
            return null;
        }
        for (UserStatusEnum value : UserStatusEnum.values()) {
            if (value.getCode() == status) {
                return value.getDescription();
            }
        }
        return "未知状态";
    }
}
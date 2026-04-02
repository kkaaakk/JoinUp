package com.joinup.credit.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.joinup.common.context.LoginUser;
import com.joinup.common.exception.BusinessException;
import com.joinup.common.exception.ErrorCode;
import com.joinup.credit.domain.CreditPermissionChecker;
import com.joinup.credit.domain.CreditRestrictionChecker;
import com.joinup.credit.domain.CreditRuleEngine;
import com.joinup.credit.domain.CreditRuleProperties;
import com.joinup.credit.dto.AdminCreditAdjustRequest;
import com.joinup.credit.dto.CreditAttendanceRewardRequest;
import com.joinup.credit.dto.CreditCancelRequest;
import com.joinup.credit.dto.CreditNoShowPenaltyRequest;
import com.joinup.credit.dto.CreditSignupCheckRequest;
import com.joinup.credit.entity.UserCreditRecordEntity;
import com.joinup.credit.enums.CreditChangeTypeEnum;
import com.joinup.credit.enums.CreditRestrictionTypeEnum;
import com.joinup.credit.mapper.UserCreditRecordMapper;
import com.joinup.credit.service.CreditService;
import com.joinup.credit.support.CreditRestrictionDecision;
import com.joinup.credit.vo.CreditRestrictionVO;
import com.joinup.credit.vo.UserCreditDetailVO;
import com.joinup.credit.vo.UserCreditRecordVO;
import com.joinup.user.entity.UserEntity;
import com.joinup.user.mapper.UserMapper;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Objects;

/**
 * 信用模块应用服务实现。
 * <p>
 * 这一层负责把“规则判断”“分值更新”“记录落表”“对外返回详情”串成完整闭环。
 * Controller 只负责接参数和回结果，真正的业务判断都集中在这里和 domain 层。
 * </p>
 */
@Service
@Validated
public class CreditServiceImpl implements CreditService {

    private static final int CREDIT_UPDATE_RETRY_TIMES = 3;

    private final UserMapper userMapper;
    private final UserCreditRecordMapper userCreditRecordMapper;
    private final CreditRuleEngine creditRuleEngine;
    private final CreditRestrictionChecker creditRestrictionChecker;
    private final CreditPermissionChecker creditPermissionChecker;
    private final CreditRuleProperties properties;

    /**
     * 创建信用模块应用服务。
     *
     * @param userMapper 用户 Mapper
     * @param userCreditRecordMapper 信用记录 Mapper
     * @param creditRuleEngine 规则计算器
     * @param creditRestrictionChecker 限制判断器
     * @param creditPermissionChecker 权限校验器
     * @param properties 信用规则配置
     */
    public CreditServiceImpl(UserMapper userMapper,
                             UserCreditRecordMapper userCreditRecordMapper,
                             CreditRuleEngine creditRuleEngine,
                             CreditRestrictionChecker creditRestrictionChecker,
                             CreditPermissionChecker creditPermissionChecker,
                             CreditRuleProperties properties) {
        this.userMapper = userMapper;
        this.userCreditRecordMapper = userCreditRecordMapper;
        this.creditRuleEngine = creditRuleEngine;
        this.creditRestrictionChecker = creditRestrictionChecker;
        this.creditPermissionChecker = creditPermissionChecker;
        this.properties = properties;
    }

    /**
     * 查询当前用户的信用详情。
     *
     * @param currentUserId 当前用户 ID
     * @return 信用详情
     */
    @Override
    public UserCreditDetailVO getCurrentUserCreditDetail(Long currentUserId) {
        return buildCreditDetail(loadUserOrThrow(currentUserId));
    }

    /**
     * 校验用户是否允许创建活动。
     *
     * @param userId 用户 ID
     */
    @Override
    public void assertCanCreateActivity(Long userId) {
        UserEntity user = loadUserOrThrow(userId);
        CreditRestrictionDecision decision = creditRestrictionChecker.evaluate(user.getId(), normalizedScore(user));
        if (!decision.isAllowCreateActivity()) {
            throw new BusinessException(ErrorCode.CREDIT_SCORE_TOO_LOW_FOR_ACTIVITY_CREATE,
                    "当前信用状态不允许创建活动，请先提升信用分后再尝试");
        }
    }

    /**
     * 校验用户是否允许报名指定活动。
     *
     * @param request 报名前信用校验请求
     */
    @Override
    public void assertCanSignup(@Valid CreditSignupCheckRequest request) {
        UserEntity user = loadUserOrThrow(request.getUserId());
        CreditRestrictionDecision decision = creditRestrictionChecker.evaluate(user.getId(), normalizedScore(user));
        if (creditRuleEngine.isHotActivity(request.getActivityHeatScore()) && !decision.isAllowHotActivitySignup()) {
            throw new BusinessException(ErrorCode.CREDIT_SCORE_TOO_LOW_FOR_HOT_ACTIVITY,
                    "当前信用状态不允许报名热门活动，请先提升信用分后再尝试");
        }
    }

    /**
     * 判断用户是否需要降低候补优先级。
     *
     * @param userId 用户 ID
     * @return 如果需要降低候补优先级，则返回 {@code true}
     */
    @Override
    public boolean shouldDeprioritizeWaitlist(Long userId) {
        UserEntity user = loadUserOrThrow(userId);
        return creditRestrictionChecker.evaluate(user.getId(), normalizedScore(user)).isDeprioritizeWaitlist();
    }

    /**
     * 记录一次守约加分。
     *
     * @param request 守约加分请求
     * @return 调整后的信用详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserCreditDetailVO recordAttendanceReward(@Valid CreditAttendanceRewardRequest request) {
        return applyCreditChange(request.getUserId(),
                CreditChangeTypeEnum.ATTEND_BONUS,
                properties.getAttendBonus(),
                request.getReason(),
                request.getActivityId(),
                request.getSignupId());
    }

    /**
     * 记录一次取消行为对应的信用变化。
     *
     * @param request 取消处理请求
     * @return 调整后的信用详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserCreditDetailVO recordSignupCancel(@Valid CreditCancelRequest request) {
        CreditChangeTypeEnum changeType =
                creditRuleEngine.resolveCancelChangeType(request.getActivityStartTime(), request.getCancelTime());
        Integer deltaScore =
                creditRuleEngine.resolveCancelDelta(request.getActivityStartTime(), request.getCancelTime());

        return applyCreditChange(request.getUserId(),
                changeType,
                deltaScore,
                request.getReason(),
                request.getActivityId(),
                request.getSignupId());
    }

    /**
     * 记录一次爽约扣分。
     *
     * @param request 爽约扣分请求
     * @return 调整后的信用详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserCreditDetailVO recordNoShowPenalty(@Valid CreditNoShowPenaltyRequest request) {
        return applyCreditChange(request.getUserId(),
                CreditChangeTypeEnum.NO_SHOW_PENALTY,
                properties.getNoShowPenalty(),
                request.getReason(),
                request.getActivityId(),
                request.getSignupId());
    }

    /**
     * 管理员人工调整用户信用分。
     *
     * @param operator 当前操作人
     * @param targetUserId 被调整用户 ID
     * @param request 调整请求
     * @return 调整后的信用详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserCreditDetailVO manualAdjust(LoginUser operator,
                                           Long targetUserId,
                                           @Valid AdminCreditAdjustRequest request) {
        creditPermissionChecker.assertAdminOperator(operator);
        if (request.getDeltaScore() == null || request.getDeltaScore() == 0) {
            throw new BusinessException(ErrorCode.CREDIT_MANUAL_ADJUST_INVALID, "人工调整分值不能为 0");
        }
        if (Math.abs(request.getDeltaScore()) > properties.getManualAdjustMaxAbsoluteDelta()) {
            throw new BusinessException(ErrorCode.CREDIT_MANUAL_ADJUST_INVALID,
                    "人工调整绝对值不能超过 " + properties.getManualAdjustMaxAbsoluteDelta());
        }
        return applyCreditChange(targetUserId,
                CreditChangeTypeEnum.MANUAL_ADJUST,
                request.getDeltaScore(),
                request.getReason(),
                null,
                null);
    }

    /**
     * 组装用户信用详情。
     *
     * @param user 用户实体
     * @return 信用详情展示对象
     */
    private UserCreditDetailVO buildCreditDetail(UserEntity user) {
        CreditRestrictionDecision decision = creditRestrictionChecker.evaluate(user.getId(), normalizedScore(user));
        List<UserCreditRecordVO> recentRecords = loadRecentRecords(user.getId());
        List<CreditRestrictionVO> restrictions = decision.getRestrictionTypes().stream()
                .map(this::mapRestriction)
                .toList();

        return UserCreditDetailVO.builder()
                .userId(user.getId())
                .creditScore(normalizedScore(user))
                .consecutiveNoShowCount(decision.getConsecutiveNoShowCount())
                .allowCreateActivity(decision.isAllowCreateActivity())
                .allowHotActivitySignup(decision.isAllowHotActivitySignup())
                .deprioritizeWaitlist(decision.isDeprioritizeWaitlist())
                .restrictions(restrictions)
                .recentRecords(recentRecords)
                .build();
    }

    /**
     * 从数据库中加载用户，不存在时直接抛业务异常。
     *
     * @param userId 用户 ID
     * @return 用户实体
     */
    private UserEntity loadUserOrThrow(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    /**
     * 读取最近的信用变更记录，并转换成对外展示对象。
     *
     * @param userId 用户 ID
     * @return 最近信用记录
     */
    private List<UserCreditRecordVO> loadRecentRecords(Long userId) {
        return userCreditRecordMapper.selectList(Wrappers.<UserCreditRecordEntity>lambdaQuery()
                        .eq(UserCreditRecordEntity::getUserId, userId)
                        .orderByDesc(UserCreditRecordEntity::getCreatedAt)
                        .last("limit " + properties.getRecentRecordLimit()))
                .stream()
                .map(this::mapRecord)
                .toList();
    }

    /**
     * 把信用限制枚举转换为对外展示对象。
     *
     * @param restrictionType 限制类型
     * @return 展示对象
     */
    private CreditRestrictionVO mapRestriction(CreditRestrictionTypeEnum restrictionType) {
        return CreditRestrictionVO.builder()
                .type(restrictionType.name())
                .description(restrictionType.getDescription())
                .build();
    }

    /**
     * 把信用记录实体转换为展示对象。
     *
     * @param entity 信用记录实体
     * @return 展示对象
     */
    private UserCreditRecordVO mapRecord(UserCreditRecordEntity entity) {
        CreditChangeTypeEnum changeType = CreditChangeTypeEnum.fromCode(entity.getChangeType());
        return UserCreditRecordVO.builder()
                .id(entity.getId())
                .changeType(entity.getChangeType())
                .changeTypeDescription(changeType != null ? changeType.getDescription() : null)
                .deltaScore(entity.getDeltaScore())
                .beforeScore(entity.getBeforeScore())
                .afterScore(entity.getAfterScore())
                .reason(entity.getReason())
                .relatedActivityId(entity.getRelatedActivityId())
                .relatedSignupId(entity.getRelatedSignupId())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * 执行一次信用分更新并写入审计记录。
     * <p>
     * 这里使用的是用户表上的乐观锁版本号，而不是数据库悲观锁。
     * 这样可以避免高频信用变化把数据库行锁拖成瓶颈，同时又能保证最终一致性。
     * 如果出现版本冲突，会做有限次重试。
     * </p>
     *
     * @param userId 用户 ID
     * @param changeType 变更类型
     * @param deltaScore 分值变化
     * @param reason 变化原因
     * @param relatedActivityId 关联活动 ID
     * @param relatedSignupId 关联报名 ID
     * @return 更新后的信用详情
     */
    private UserCreditDetailVO applyCreditChange(Long userId,
                                                 CreditChangeTypeEnum changeType,
                                                 Integer deltaScore,
                                                 String reason,
                                                 Long relatedActivityId,
                                                 Long relatedSignupId) {
        String finalReason = StringUtils.hasText(reason) ? reason.trim() : buildDefaultReason(changeType);

        for (int attempt = 1; attempt <= CREDIT_UPDATE_RETRY_TIMES; attempt++) {
            UserEntity user = loadUserOrThrow(userId);
            Integer beforeScore = normalizedScore(user);
            Integer afterScore = creditRuleEngine.clampScore(beforeScore + deltaScore);

            user.setCreditScore(afterScore);
            int updatedRows = userMapper.updateById(user);
            if (updatedRows != 1) {
                continue;
            }

            UserCreditRecordEntity record = new UserCreditRecordEntity();
            record.setUserId(userId);
            record.setChangeType(changeType.getCode());
            record.setDeltaScore(deltaScore);
            record.setBeforeScore(beforeScore);
            record.setAfterScore(afterScore);
            record.setReason(finalReason);
            record.setRelatedActivityId(relatedActivityId);
            record.setRelatedSignupId(relatedSignupId);
            userCreditRecordMapper.insert(record);

            return buildCreditDetail(loadUserOrThrow(userId));
        }

        throw new BusinessException(ErrorCode.CONFLICT, "信用分更新冲突，请稍后重试");
    }

    /**
     * 为没有传入原因的场景生成默认文案。
     *
     * @param changeType 变更类型
     * @return 默认原因说明
     */
    private String buildDefaultReason(CreditChangeTypeEnum changeType) {
        if (Objects.equals(changeType, CreditChangeTypeEnum.ATTEND_BONUS)) {
            return "正常参加活动，信用分增加";
        }
        if (Objects.equals(changeType, CreditChangeTypeEnum.NORMAL_CANCEL)) {
            return "在允许时间内取消报名";
        }
        if (Objects.equals(changeType, CreditChangeTypeEnum.LATE_CANCEL_PENALTY)) {
            return "活动开始前临时取消报名";
        }
        if (Objects.equals(changeType, CreditChangeTypeEnum.NO_SHOW_PENALTY)) {
            return "活动开始后未到场，记为爽约";
        }
        if (Objects.equals(changeType, CreditChangeTypeEnum.MANUAL_ADJUST)) {
            return "管理员人工调整信用分";
        }
        return "信用分发生变化";
    }

    /**
     * 统一读取用户当前信用分。
     *
     * @param user 用户实体
     * @return 规范化后的信用分
     */
    private Integer normalizedScore(UserEntity user) {
        return creditRuleEngine.clampScore(user.getCreditScore() == null ? properties.getInitialScore() : user.getCreditScore());
    }
}
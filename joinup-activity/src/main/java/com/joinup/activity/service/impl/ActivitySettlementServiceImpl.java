package com.joinup.activity.service.impl;

import com.joinup.activity.domain.ActivitySettlementLockService;
import com.joinup.activity.domain.ActivitySettlementRule;
import com.joinup.activity.domain.ActivityStatusLogDomainService;
import com.joinup.activity.entity.ActivityEntity;
import com.joinup.activity.enums.ActivityStatusEnum;
import com.joinup.activity.mapper.ActivityMapper;
import com.joinup.activity.service.ActivitySettlementService;
import com.joinup.activity.domain.ActivitySettlementProperties;
import com.joinup.activity.domain.ActivityStatusFlow;
import com.joinup.common.event.DomainEventPublisher;
import com.joinup.common.event.activity.ActivitySettlementCompletedEvent;
import com.joinup.common.exception.BusinessException;
import com.joinup.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 活动报名截止结算服务实现。
 * <p>
 * 该服务负责在报名截止后自动判定活动是否成团，并完成：
 * 1. 活动状态变更；
 * 2. 活动状态日志写入；
 * 3. 事务后 Kafka 事件发布。
 * </p>
 */
@Service
public class ActivitySettlementServiceImpl implements ActivitySettlementService {

    private static final Long SYSTEM_OPERATOR_ID = 0L;

    private final ActivityMapper activityMapper;
    private final ActivityStatusLogDomainService activityStatusLogDomainService;
    private final ActivitySettlementLockService activitySettlementLockService;
    private final DomainEventPublisher domainEventPublisher;
    private final ActivitySettlementProperties activitySettlementProperties;

    /**
     * 构造活动报名截止结算服务实现。
     *
     * @param activityMapper 活动表访问器
     * @param activityStatusLogDomainService 活动状态日志领域服务
     * @param activitySettlementLockService 活动结算锁服务
     * @param domainEventPublisher 领域事件发布器
     * @param activitySettlementProperties 活动结算配置属性
     */
    public ActivitySettlementServiceImpl(ActivityMapper activityMapper,
                                         ActivityStatusLogDomainService activityStatusLogDomainService,
                                         ActivitySettlementLockService activitySettlementLockService,
                                         DomainEventPublisher domainEventPublisher,
                                         ActivitySettlementProperties activitySettlementProperties) {
        this.activityMapper = activityMapper;
        this.activityStatusLogDomainService = activityStatusLogDomainService;
        this.activitySettlementLockService = activitySettlementLockService;
        this.domainEventPublisher = domainEventPublisher;
        this.activitySettlementProperties = activitySettlementProperties;
    }

    /**
     * 扫描并结算所有已到报名截止时间的活动。
     * <p>
     * 这里先拿全局扫描锁，保证多实例部署下只有一个节点执行批量扫描。
     * 真正结算单个活动时，还会再拿活动级锁做二次保护。
     * </p>
     */
    @Override
    public void scanAndSettleDueActivities() {
        activitySettlementLockService.executeWithScanLock(() -> {
            List<ActivityEntity> dueActivities = activityMapper.selectDueForSettlement(
                    LocalDateTime.now(),
                    activitySettlementProperties.getScanBatchSize());
            for (ActivityEntity activity : dueActivities) {
                settleActivity(activity.getId(), SYSTEM_OPERATOR_ID, "报名截止自动结算");
            }
        });
    }

    /**
     * 结算单个活动。
     * <p>
     * 该方法会先拿活动级锁，再进入事务内执行真正的结算逻辑，确保同一个活动不会被重复结算。
     * </p>
     *
     * @param activityId 活动 ID
     * @param operatorId 操作人 ID
     * @param triggerReason 触发原因说明
     */
    @Override
    public void settleActivity(Long activityId, Long operatorId, String triggerReason) {
        activitySettlementLockService.executeWithActivityLock(activityId,
                () -> doSettleActivity(activityId, operatorId, triggerReason));
    }

    /**
     * 在事务内执行活动结算。
     *
     * @param activityId 活动 ID
     * @param operatorId 操作人 ID
     * @param triggerReason 触发原因说明
     */
    @Transactional(rollbackFor = Exception.class)
    protected void doSettleActivity(Long activityId, Long operatorId, String triggerReason) {
        ActivityEntity activity = getActivityOrThrow(activityId);
        ActivityStatusEnum currentStatus = ActivitySettlementRule.statusOf(activity.getStatus());
        if (!ActivitySettlementRule.canSettle(currentStatus)) {
            return;
        }
        if (!ActivitySettlementRule.isDeadlineReached(activity.getSignupDeadline(), LocalDateTime.now())) {
            return;
        }

        ActivityStatusEnum targetStatus = ActivitySettlementRule.evaluateSettlementResult(activity);
        ActivityStatusFlow.assertTransition(currentStatus, targetStatus);

        activity.setStatus(targetStatus.getCode());
        activityMapper.updateById(activity);

        String reason = ActivitySettlementRule.buildSettlementReason(
                targetStatus,
                activity.getCurrentParticipants(),
                activity.getMinGroupSize(),
                triggerReason);

        activityStatusLogDomainService.recordStatusChange(
                activity.getId(),
                currentStatus,
                targetStatus,
                reason,
                operatorId == null ? SYSTEM_OPERATOR_ID : operatorId);

        domainEventPublisher.publish(ActivitySettlementCompletedEvent.builder()
                .eventId(newEventId())
                .activityId(activity.getId())
                .organizerId(activity.getOrganizerId())
                .fromStatus(currentStatus.getCode())
                .toStatus(targetStatus.getCode())
                .currentParticipants(activity.getCurrentParticipants())
                .minGroupSize(activity.getMinGroupSize())
                .rosterLocked(targetStatus == ActivityStatusEnum.GROUP_SUCCESS)
                .closeSignups(targetStatus == ActivityStatusEnum.GROUP_FAILED)
                .closeWaitlists(Boolean.TRUE)
                .reason(reason)
                .occurredAt(LocalDateTime.now())
                .build());
    }

    /**
     * 加载活动实体并校验活动存在性。
     *
     * @param activityId 活动 ID
     * @return 活动实体
     */
    private ActivityEntity getActivityOrThrow(Long activityId) {
        ActivityEntity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND);
        }
        return activity;
    }

    /**
     * 生成活动结算事件唯一标识。
     *
     * @return 去掉中划线的 UUID 字符串
     */
    private String newEventId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
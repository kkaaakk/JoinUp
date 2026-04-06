package com.joinup.activity.domain;

import com.joinup.activity.entity.ActivityStatusLogEntity;
import com.joinup.activity.enums.ActivityStatusEnum;
import com.joinup.activity.mapper.ActivityStatusLogMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 活动状态日志领域服务。
 * <p>
 * 该服务专门负责把活动状态变化落到 `activity_status_log`，
 * 避免状态日志插入逻辑在各个业务入口重复出现。
 * </p>
 */
@Component
public class ActivityStatusLogDomainService {

    private final ActivityStatusLogMapper activityStatusLogMapper;

    /**
     * 构造活动状态日志领域服务。
     *
     * @param activityStatusLogMapper 活动状态日志访问器
     */
    public ActivityStatusLogDomainService(ActivityStatusLogMapper activityStatusLogMapper) {
        this.activityStatusLogMapper = activityStatusLogMapper;
    }

    /**
     * 记录一次活动状态变更日志。
     *
     * @param activityId 活动 ID
     * @param fromStatus 变更前状态
     * @param toStatus 变更后状态
     * @param reason 变更原因
     * @param operatorId 操作人 ID，系统任务可传 0
     */
    public void recordStatusChange(Long activityId,
                                   ActivityStatusEnum fromStatus,
                                   ActivityStatusEnum toStatus,
                                   String reason,
                                   Long operatorId) {
        ActivityStatusLogEntity logEntity = new ActivityStatusLogEntity();
        logEntity.setActivityId(activityId);
        logEntity.setFromStatus(fromStatus.getCode());
        logEntity.setToStatus(toStatus.getCode());
        logEntity.setReason(reason);
        logEntity.setOperatorId(operatorId == null ? 0L : operatorId);
        logEntity.setOccurredAt(LocalDateTime.now());
        activityStatusLogMapper.insert(logEntity);
    }
}
package com.joinup.signup.domain;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.joinup.activity.entity.ActivityEntity;
import com.joinup.activity.enums.ActivityStatusEnum;
import com.joinup.activity.mapper.ActivityMapper;
import com.joinup.common.event.DomainEventPublisher;
import com.joinup.common.event.signup.FormalSignupCanceledEvent;
import com.joinup.common.event.waitlist.WaitlistPromotionExpiredEvent;
import com.joinup.common.event.waitlist.WaitlistPromotionOfferedEvent;
import com.joinup.common.exception.BusinessException;
import com.joinup.common.exception.ErrorCode;
import com.joinup.signup.entity.ActivitySignupEntity;
import com.joinup.signup.enums.SignupSourceEnum;
import com.joinup.signup.enums.SignupStatusEnum;
import com.joinup.signup.mapper.ActivitySignupMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * 候补补位与报名模块衔接服务。
 * <p>
 * 该服务负责把候补模块发出的“补位发放 / 补位超时”事件落到报名模块自己的数据结构中，
 * 包括正式报名记录、活动当前人数以及 Redis 正式名额缓存。
 * </p>
 */
@Service
public class WaitlistPromotionSignupCoordinator {

    private final ActivityMapper activityMapper;
    private final ActivitySignupMapper activitySignupMapper;
    private final SignupActivityCacheService signupActivityCacheService;
    private final DomainEventPublisher domainEventPublisher;

    /**
     * 构造候补补位与报名衔接服务。
     *
     * @param activityMapper 活动表访问器
     * @param activitySignupMapper 正式报名表访问器
     * @param signupActivityCacheService 报名缓存服务
     * @param domainEventPublisher 领域事件发布器
     */
    public WaitlistPromotionSignupCoordinator(ActivityMapper activityMapper,
                                              ActivitySignupMapper activitySignupMapper,
                                              SignupActivityCacheService signupActivityCacheService,
                                              DomainEventPublisher domainEventPublisher) {
        this.activityMapper = activityMapper;
        this.activitySignupMapper = activitySignupMapper;
        this.signupActivityCacheService = signupActivityCacheService;
        this.domainEventPublisher = domainEventPublisher;
    }

    /**
     * 处理候补补位发放事件。
     * <p>
     * 当候补模块已经选出下一位候补用户并发放补位机会时，报名模块需要：
     * 1. 创建或恢复正式报名记录；
     * 2. 占用重新释放出来的正式席位；
     * 3. 把 Redis 正式名额再次扣回去。
     * </p>
     *
     * @param event 候补补位发放事件
     */
    @Transactional(rollbackFor = Exception.class)
    public void handlePromotionOffered(WaitlistPromotionOfferedEvent event) {
        ActivityEntity activity = getActivity(event.getActivityId());
        ActivitySignupEntity signupEntity = findSignup(event.getActivityId(), event.getUserId());
        boolean needsParticipantIncrease = false;
        if (signupEntity == null) {
            signupEntity = new ActivitySignupEntity();
            signupEntity.setActivityId(event.getActivityId());
            signupEntity.setUserId(event.getUserId());
            signupEntity.setStatus(SignupStatusEnum.PROMOTED.getCode());
            signupEntity.setSource(SignupSourceEnum.WAITLIST_PROMOTION.getCode());
            signupEntity.setSignupTime(LocalDateTime.now());
            activitySignupMapper.insert(signupEntity);
            needsParticipantIncrease = true;
        } else if (Objects.equals(signupEntity.getStatus(), SignupStatusEnum.CANCELED.getCode())) {
            signupEntity.setStatus(SignupStatusEnum.PROMOTED.getCode());
            signupEntity.setSource(SignupSourceEnum.WAITLIST_PROMOTION.getCode());
            signupEntity.setSignupTime(LocalDateTime.now());
            signupEntity.setCancelTime(null);
            signupEntity.setCancelReason(null);
            activitySignupMapper.updateById(signupEntity);
            needsParticipantIncrease = true;
        }
        if (needsParticipantIncrease) {
            activity.setCurrentParticipants(safeInt(activity.getCurrentParticipants()) + 1);
            refreshActivityStatusAfterFormalChange(activity);
            activityMapper.updateById(activity);
        }
        signupActivityCacheService.markPromotionReserved(event.getActivityId(), event.getUserId(), activity);
    }

    /**
     * 处理候补补位超时事件。
     * <p>
     * 当候补补位超时后，报名模块需要回收之前为该候补用户保留的正式席位，
     * 并再次发布正式席位释放事件，以驱动候补模块继续顺延下一位。
     * </p>
     *
     * @param event 候补补位超时事件
     */
    @Transactional(rollbackFor = Exception.class)
    public void handlePromotionExpired(WaitlistPromotionExpiredEvent event) {
        ActivityEntity activity = getActivity(event.getActivityId());
        ActivitySignupEntity signupEntity = findSignup(event.getActivityId(), event.getUserId());
        if (signupEntity != null && Objects.equals(signupEntity.getStatus(), SignupStatusEnum.PROMOTED.getCode())) {
            signupEntity.setStatus(SignupStatusEnum.CANCELED.getCode());
            signupEntity.setCancelTime(LocalDateTime.now());
            signupEntity.setCancelReason("候补补位确认超时");
            activitySignupMapper.updateById(signupEntity);
            activity.setCurrentParticipants(Math.max(0, safeInt(activity.getCurrentParticipants()) - 1));
            refreshActivityStatusAfterFormalChange(activity);
            activityMapper.updateById(activity);
        }
        signupActivityCacheService.markPromotionExpired(event.getActivityId(), event.getUserId(), activity);
        domainEventPublisher.publish(FormalSignupCanceledEvent.builder()
                .eventId(newEventId())
                .activityId(event.getActivityId())
                .canceledUserId(event.getUserId())
                .cancelReason("候补补位确认超时")
                .occurredAt(LocalDateTime.now())
                .build());
    }

    /**
     * 加载活动实体。
     *
     * @param activityId 活动 ID
     * @return 活动实体
     */
    private ActivityEntity getActivity(Long activityId) {
        ActivityEntity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND);
        }
        return activity;
    }

    /**
     * 查询指定活动下的正式报名记录。
     *
     * @param activityId 活动 ID
     * @param userId 用户 ID
     * @return 正式报名记录；若不存在则返回 {@code null}
     */
    private ActivitySignupEntity findSignup(Long activityId, Long userId) {
        return activitySignupMapper.selectOne(new LambdaQueryWrapper<ActivitySignupEntity>()
                .eq(ActivitySignupEntity::getActivityId, activityId)
                .eq(ActivitySignupEntity::getUserId, userId)
                .last("limit 1"));
    }

    /**
     * 在正式名额变化后刷新活动状态。
     *
     * @param activity 活动实体
     */
    private void refreshActivityStatusAfterFormalChange(ActivityEntity activity) {
        int currentParticipants = safeInt(activity.getCurrentParticipants());
        int maxParticipants = safeInt(activity.getMaxParticipants());
        boolean allowWaitlist = safeInt(activity.getAllowWaitlist()) == 1;
        int waitlistCount = safeInt(activity.getWaitlistCount());
        if (currentParticipants >= maxParticipants) {
            activity.setStatus(allowWaitlist && waitlistCount > 0
                    ? ActivityStatusEnum.WAITLIST_OPEN.getCode()
                    : ActivityStatusEnum.FULL.getCode());
            return;
        }
        if (allowWaitlist && waitlistCount > 0) {
            activity.setStatus(ActivityStatusEnum.WAITLIST_OPEN.getCode());
            return;
        }
        activity.setStatus(ActivityStatusEnum.SIGNUP_OPEN.getCode());
    }

    /**
     * 把可空整数安全转换为非空值。
     *
     * @param value 原始整数
     * @return 非空整数；为空时返回 0
     */
    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 生成领域事件唯一标识。
     *
     * @return 去掉中划线的 UUID 字符串
     */
    private String newEventId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}

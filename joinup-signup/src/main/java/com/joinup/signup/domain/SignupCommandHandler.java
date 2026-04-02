package com.joinup.signup.domain;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.joinup.activity.entity.ActivityEntity;
import com.joinup.activity.enums.ActivityStatusEnum;
import com.joinup.activity.mapper.ActivityMapper;
import com.joinup.common.event.DomainEventPublisher;
import com.joinup.common.event.signup.FormalSignupCanceledEvent;
import com.joinup.common.event.waitlist.WaitlistCanceledEvent;
import com.joinup.common.event.waitlist.WaitlistJoinedEvent;
import com.joinup.common.exception.BusinessException;
import com.joinup.common.exception.ErrorCode;
import com.joinup.signup.entity.ActivitySignupEntity;
import com.joinup.signup.enums.SignupCommandTypeEnum;
import com.joinup.signup.enums.SignupReservationTypeEnum;
import com.joinup.signup.enums.SignupSourceEnum;
import com.joinup.signup.enums.SignupStatusEnum;
import com.joinup.signup.event.SignupCommandEvent;
import com.joinup.signup.mapper.ActivitySignupMapper;
import com.joinup.waitlist.entity.ActivityWaitlistEntity;
import com.joinup.waitlist.enums.WaitlistStatusEnum;
import com.joinup.waitlist.mapper.ActivityWaitlistMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * 报名命令领域处理器。
 * <p>
 * 当 Redis 已经完成正式名额或候补名额的预占之后，真正的数据库落库、人数更新、
 * 活动状态刷新等事务性操作由这个类统一负责。
 * </p>
 */
@Component
public class SignupCommandHandler {

    private final ActivityMapper activityMapper;
    private final ActivitySignupMapper activitySignupMapper;
    private final ActivityWaitlistMapper activityWaitlistMapper;
    private final SignupActivityCacheService signupActivityCacheService;
    private final DomainEventPublisher domainEventPublisher;

    /**
     * 构造报名命令处理器。
     *
     * @param activityMapper 活动表访问器
     * @param activitySignupMapper 正式报名表访问器
     * @param activityWaitlistMapper 候补表访问器
     * @param signupActivityCacheService Redis 缓存状态服务
     * @param domainEventPublisher 领域事件发布器
     */
    public SignupCommandHandler(ActivityMapper activityMapper,
                                ActivitySignupMapper activitySignupMapper,
                                ActivityWaitlistMapper activityWaitlistMapper,
                                SignupActivityCacheService signupActivityCacheService,
                                DomainEventPublisher domainEventPublisher) {
        this.activityMapper = activityMapper;
        this.activitySignupMapper = activitySignupMapper;
        this.activityWaitlistMapper = activityWaitlistMapper;
        this.signupActivityCacheService = signupActivityCacheService;
        this.domainEventPublisher = domainEventPublisher;
    }

    /**
     * 统一处理报名命令。
     * <p>
     * 该方法运行在数据库事务中，保证活动人数、正式报名记录、候补记录等相关数据要么一起成功，
     * 要么一起失败回滚。
     * </p>
     *
     * @param event 报名命令事件
     */
    @Transactional(rollbackFor = Exception.class)
    public void handle(SignupCommandEvent event) {
        if (event.getCommandType() == SignupCommandTypeEnum.APPLY) {
            handleApply(event);
            return;
        }
        handleCancel(event);
    }

    /**
     * 处理报名申请命令。
     * <p>
     * 到达这里时，Redis 已经完成席位预占，因此当前的重点是把预占结果持久化为数据库事实，
     * 并在成功后通知缓存服务更新最终状态。
     * </p>
     *
     * @param event 报名申请命令
     */
    private void handleApply(SignupCommandEvent event) {
        ActivityEntity activity = getActivity(event.getActivityId());
        try {
            if (event.getReservationType() == SignupReservationTypeEnum.FORMAL) {
                persistFormalSignup(event, activity);
            } else {
                persistWaitlistSignup(event, activity);
            }
        } catch (DuplicateKeyException ex) {
            if (!isAlreadyPersisted(event)) {
                throw ex;
            }
        }
        // 如果数据库里已经是目标状态，则把这次重复消费视为幂等成功；
        // 即便如此，仍然要刷新 Redis 结果，确保缓存侧也收敛到最终成功状态。
        signupActivityCacheService.markApplySuccess(event, activity);
    }

    /**
     * 处理取消报名命令。
     * <p>
     * 取消报名也通过异步消费者完成，目的是避免同步接口线程直接参与数据库更新，
     * 从而减少热点活动场景下的锁竞争。
     * </p>
     *
     * @param event 取消报名命令
     */
    private void handleCancel(SignupCommandEvent event) {
        ActivityEntity activity = getActivity(event.getActivityId());
        if (event.getReservationType() == SignupReservationTypeEnum.FORMAL) {
            cancelFormalSignup(event, activity);
        } else {
            cancelWaitlistSignup(event, activity);
        }
        signupActivityCacheService.markCancelSuccess(event, activity);
    }

    /**
     * 持久化正式报名。
     * <p>
     * 只有当数据库中的状态真正从“未报名/已取消”进入“有效报名”时，
     * 才会增加活动当前人数，避免重复消息造成重复累加。
     * </p>
     *
     * @param event 报名命令
     * @param activity 当前活动实体
     */
    private void persistFormalSignup(SignupCommandEvent event, ActivityEntity activity) {
        ActivitySignupEntity signupEntity = findSignup(event.getActivityId(), event.getUserId());
        boolean needsParticipantIncrease = false;
        if (signupEntity == null) {
            signupEntity = new ActivitySignupEntity();
            signupEntity.setActivityId(event.getActivityId());
            signupEntity.setUserId(event.getUserId());
            signupEntity.setStatus(SignupStatusEnum.SIGNED.getCode());
            signupEntity.setSource(SignupSourceEnum.DIRECT.getCode());
            signupEntity.setSignupTime(LocalDateTime.now());
            activitySignupMapper.insert(signupEntity);
            needsParticipantIncrease = true;
        } else if (Objects.equals(signupEntity.getStatus(), SignupStatusEnum.CANCELED.getCode())) {
            signupEntity.setStatus(SignupStatusEnum.SIGNED.getCode());
            signupEntity.setSource(SignupSourceEnum.DIRECT.getCode());
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
    }

    /**
     * 持久化候补报名。
     * <p>
     * 候补记录独立写入候补表，这样正式报名和候补排队可以分别演进，
     * 也为后续实现“取消后自动补位”保留了清晰的数据结构。
     * </p>
     *
     * @param event 报名命令
     * @param activity 当前活动实体
     */
    private void persistWaitlistSignup(SignupCommandEvent event, ActivityEntity activity) {
        ActivityWaitlistEntity waitlistEntity = findWaitlist(event.getActivityId(), event.getUserId());
        boolean needsWaitlistIncrease = false;
        if (waitlistEntity == null) {
            waitlistEntity = new ActivityWaitlistEntity();
            waitlistEntity.setActivityId(event.getActivityId());
            waitlistEntity.setUserId(event.getUserId());
            waitlistEntity.setQueueNo(event.getQueueNo());
            waitlistEntity.setStatus(WaitlistStatusEnum.QUEUED.getCode());
            waitlistEntity.setJoinedAt(LocalDateTime.now());
            activityWaitlistMapper.insert(waitlistEntity);
            needsWaitlistIncrease = true;
        } else if (Objects.equals(waitlistEntity.getStatus(), WaitlistStatusEnum.CANCELED.getCode())
                || Objects.equals(waitlistEntity.getStatus(), WaitlistStatusEnum.EXPIRED.getCode())) {
            waitlistEntity.setQueueNo(event.getQueueNo());
            waitlistEntity.setStatus(WaitlistStatusEnum.QUEUED.getCode());
            waitlistEntity.setJoinedAt(LocalDateTime.now());
            waitlistEntity.setPromotedAt(null);
            waitlistEntity.setConfirmedAt(null);
            waitlistEntity.setConfirmDeadline(null);
            waitlistEntity.setExpiredAt(null);
            waitlistEntity.setPromotionSource(null);
            activityWaitlistMapper.updateById(waitlistEntity);
            needsWaitlistIncrease = true;
        }
        if (needsWaitlistIncrease) {
            activity.setWaitlistCount(safeInt(activity.getWaitlistCount()) + 1);
            refreshActivityStatusAfterWaitlistChange(activity);
            activityMapper.updateById(activity);
            domainEventPublisher.publish(WaitlistJoinedEvent.builder()
                    .eventId(newEventId())
                    .activityId(event.getActivityId())
                    .userId(event.getUserId())
                    .queueNo(waitlistEntity.getQueueNo())
                    .occurredAt(LocalDateTime.now())
                    .build());
        }
    }

    /**
     * 取消正式报名。
     * <p>
     * 正式报名取消后，需要更新报名状态、记录取消时间和取消原因，
     * 同时回收活动当前人数并刷新活动状态。
     * </p>
     *
     * @param event 取消命令
     * @param activity 当前活动实体
     */
    private void cancelFormalSignup(SignupCommandEvent event, ActivityEntity activity) {
        ActivitySignupEntity signupEntity = findSignup(event.getActivityId(), event.getUserId());
        if (signupEntity == null) {
            throw new BusinessException(ErrorCode.SIGNUP_NOT_FOUND);
        }
        if (Objects.equals(signupEntity.getStatus(), SignupStatusEnum.CANCELED.getCode())) {
            return;
        }
        if (!Objects.equals(signupEntity.getStatus(), SignupStatusEnum.SIGNED.getCode())
                && !Objects.equals(signupEntity.getStatus(), SignupStatusEnum.PROMOTED.getCode())) {
            throw new BusinessException(ErrorCode.SIGNUP_CANCEL_NOT_ALLOWED);
        }
        signupEntity.setStatus(SignupStatusEnum.CANCELED.getCode());
        signupEntity.setCancelTime(LocalDateTime.now());
        signupEntity.setCancelReason(event.getCancelReason());
        activitySignupMapper.updateById(signupEntity);

        activity.setCurrentParticipants(Math.max(0, safeInt(activity.getCurrentParticipants()) - 1));
        refreshActivityStatusAfterFormalChange(activity);
        activityMapper.updateById(activity);
        domainEventPublisher.publish(FormalSignupCanceledEvent.builder()
                .eventId(newEventId())
                .activityId(event.getActivityId())
                .canceledUserId(event.getUserId())
                .cancelReason(event.getCancelReason())
                .occurredAt(LocalDateTime.now())
                .build());
    }

    /**
     * 取消候补报名。
     * <p>
     * 候补取消不会影响正式报名人数，只需要减少候补人数并重新评估活动状态。
     * </p>
     *
     * @param event 取消命令
     * @param activity 当前活动实体
     */
    private void cancelWaitlistSignup(SignupCommandEvent event, ActivityEntity activity) {
        ActivityWaitlistEntity waitlistEntity = findWaitlist(event.getActivityId(), event.getUserId());
        if (waitlistEntity == null) {
            throw new BusinessException(ErrorCode.SIGNUP_NOT_FOUND);
        }
        if (Objects.equals(waitlistEntity.getStatus(), WaitlistStatusEnum.CANCELED.getCode())) {
            return;
        }
        if (!Objects.equals(waitlistEntity.getStatus(), WaitlistStatusEnum.QUEUED.getCode())) {
            throw new BusinessException(ErrorCode.SIGNUP_CANCEL_NOT_ALLOWED);
        }
        waitlistEntity.setStatus(WaitlistStatusEnum.CANCELED.getCode());
        waitlistEntity.setExpiredAt(LocalDateTime.now());
        activityWaitlistMapper.updateById(waitlistEntity);

        activity.setWaitlistCount(Math.max(0, safeInt(activity.getWaitlistCount()) - 1));
        refreshActivityStatusAfterWaitlistChange(activity);
        activityMapper.updateById(activity);
        domainEventPublisher.publish(WaitlistCanceledEvent.builder()
                .eventId(newEventId())
                .activityId(event.getActivityId())
                .userId(event.getUserId())
                .occurredAt(LocalDateTime.now())
                .build());
    }

    /**
     * 加载活动实体并校验其存在性。
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
     * 查询用户在指定活动上的正式报名记录。
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
     * 查询用户在指定活动上的候补记录。
     *
     * @param activityId 活动 ID
     * @param userId 用户 ID
     * @return 候补记录；若不存在则返回 {@code null}
     */
    private ActivityWaitlistEntity findWaitlist(Long activityId, Long userId) {
        return activityWaitlistMapper.selectOne(new LambdaQueryWrapper<ActivityWaitlistEntity>()
                .eq(ActivityWaitlistEntity::getActivityId, activityId)
                .eq(ActivityWaitlistEntity::getUserId, userId)
                .last("limit 1"));
    }

    /**
     * 判断当前命令是否已经被成功持久化过。
     * <p>
     * Kafka 在异常情况下可能出现重复投递，数据库最终状态才是判断是否可视为幂等成功的权威依据。
     * </p>
     *
     * @param event 报名命令事件
     * @return 如果目标状态已经存在，则返回 {@code true}
     */
    private boolean isAlreadyPersisted(SignupCommandEvent event) {
        if (event.getReservationType() == SignupReservationTypeEnum.FORMAL) {
            ActivitySignupEntity existing = findSignup(event.getActivityId(), event.getUserId());
            return existing != null
                    && (Objects.equals(existing.getStatus(), SignupStatusEnum.SIGNED.getCode())
                    || Objects.equals(existing.getStatus(), SignupStatusEnum.PROMOTED.getCode()));
        }
        ActivityWaitlistEntity existing = findWaitlist(event.getActivityId(), event.getUserId());
        return existing != null && Objects.equals(existing.getStatus(), WaitlistStatusEnum.QUEUED.getCode());
    }

    /**
     * 在正式报名人数变化后刷新活动状态。
     * <p>
     * 这里根据当前人数、最大人数、是否允许候补和候补人数重新推导活动状态，
     * 保证状态转换规则集中且确定。
     * </p>
     *
     * @param activity 当前活动实体
     */
    private void refreshActivityStatusAfterFormalChange(ActivityEntity activity) {
        int currentParticipants = safeInt(activity.getCurrentParticipants());
        int maxParticipants = safeInt(activity.getMaxParticipants());
        boolean allowWaitlist = safeInt(activity.getAllowWaitlist()) == 1;
        int waitlistCount = safeInt(activity.getWaitlistCount());
        if (currentParticipants >= maxParticipants) {
            activity.setStatus(allowWaitlist ? ActivityStatusEnum.WAITLIST_OPEN.getCode() : ActivityStatusEnum.FULL.getCode());
            return;
        }
        if (allowWaitlist && waitlistCount > 0) {
            activity.setStatus(ActivityStatusEnum.WAITLIST_OPEN.getCode());
            return;
        }
        activity.setStatus(ActivityStatusEnum.SIGNUP_OPEN.getCode());
    }

    /**
     * 在候补人数变化后刷新活动状态。
     * <p>
     * 当正式名额已满且允许候补时，候补人数的变化会影响活动是表现为 FULL 还是 WAITLIST_OPEN。
     * </p>
     *
     * @param activity 当前活动实体
     */
    private void refreshActivityStatusAfterWaitlistChange(ActivityEntity activity) {
        int currentParticipants = safeInt(activity.getCurrentParticipants());
        int maxParticipants = safeInt(activity.getMaxParticipants());
        boolean allowWaitlist = safeInt(activity.getAllowWaitlist()) == 1;
        int waitlistCount = safeInt(activity.getWaitlistCount());
        if (currentParticipants >= maxParticipants) {
            activity.setStatus(allowWaitlist && waitlistCount > 0
                    ? ActivityStatusEnum.WAITLIST_OPEN.getCode()
                    : (allowWaitlist ? ActivityStatusEnum.WAITLIST_OPEN.getCode() : ActivityStatusEnum.FULL.getCode()));
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

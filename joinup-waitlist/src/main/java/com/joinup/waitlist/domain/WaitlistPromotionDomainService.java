package com.joinup.waitlist.domain;

import com.joinup.activity.entity.ActivityEntity;
import com.joinup.activity.enums.ActivityStatusEnum;
import com.joinup.activity.mapper.ActivityMapper;
import com.joinup.common.event.DomainEventPublisher;
import com.joinup.common.event.signup.FormalSignupCanceledEvent;
import com.joinup.common.event.waitlist.WaitlistCanceledEvent;
import com.joinup.common.event.waitlist.WaitlistJoinedEvent;
import com.joinup.common.event.waitlist.WaitlistPromotionExpiredEvent;
import com.joinup.common.event.waitlist.WaitlistPromotionOfferedEvent;
import com.joinup.notify.entity.NotifyMessageEntity;
import com.joinup.notify.enums.NotifyChannelEnum;
import com.joinup.notify.enums.NotifyMessageTypeEnum;
import com.joinup.notify.enums.NotifyStatusEnum;
import com.joinup.notify.mapper.NotifyMessageMapper;
import com.joinup.waitlist.entity.ActivityWaitlistEntity;
import com.joinup.waitlist.enums.WaitlistPromotionSourceEnum;
import com.joinup.waitlist.enums.WaitlistStatusEnum;
import com.joinup.waitlist.mapper.ActivityWaitlistMapper;
import com.joinup.waitlist.support.WaitlistQueueMember;
import com.joinup.waitlist.support.WaitlistRedisKeys;
import com.joinup.waitlist.support.WaitlistTimeoutMember;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 候补补位领域服务。
 * <p>
 * 该服务负责处理候补模块最核心的业务动作：
 * 1. 候补入队后的 Redis 队列同步；
 * 2. 正式席位释放后的自动补位推进；
 * 3. 补位待确认超时后的顺延；
 * 4. 候补补位通知写入。
 * </p>
 */
@Service
public class WaitlistPromotionDomainService {

    private final ActivityWaitlistMapper activityWaitlistMapper;
    private final ActivityMapper activityMapper;
    private final NotifyMessageMapper notifyMessageMapper;
    private final DomainEventPublisher domainEventPublisher;
    private final WaitlistRedisQueueService waitlistRedisQueueService;
    private final RedissonClient redissonClient;
    private final WaitlistProperties waitlistProperties;

    /**
     * 构造候补补位领域服务。
     *
     * @param activityWaitlistMapper 候补表访问器
     * @param activityMapper 活动表访问器
     * @param notifyMessageMapper 通知表访问器
     * @param domainEventPublisher 领域事件发布器
     * @param waitlistRedisQueueService 候补 Redis 队列服务
     * @param redissonClient Redisson 客户端
     * @param waitlistProperties 候补模块配置
     */
    public WaitlistPromotionDomainService(ActivityWaitlistMapper activityWaitlistMapper,
                                          ActivityMapper activityMapper,
                                          NotifyMessageMapper notifyMessageMapper,
                                          DomainEventPublisher domainEventPublisher,
                                          WaitlistRedisQueueService waitlistRedisQueueService,
                                          RedissonClient redissonClient,
                                          WaitlistProperties waitlistProperties) {
        this.activityWaitlistMapper = activityWaitlistMapper;
        this.activityMapper = activityMapper;
        this.notifyMessageMapper = notifyMessageMapper;
        this.domainEventPublisher = domainEventPublisher;
        this.waitlistRedisQueueService = waitlistRedisQueueService;
        this.redissonClient = redissonClient;
        this.waitlistProperties = waitlistProperties;
    }

    /**
     * 处理候补入队后的缓存同步。
     *
     * @param event 候补入队事件
     */
    public void handleWaitlistJoined(WaitlistJoinedEvent event) {
        ActivityWaitlistEntity entity = activityWaitlistMapper.selectByActivityIdAndUserId(event.getActivityId(), event.getUserId());
        if (entity == null || !Objects.equals(entity.getStatus(), WaitlistStatusEnum.QUEUED.getCode())) {
            return;
        }
        ActivityEntity activity = activityMapper.selectById(event.getActivityId());
        if (activity == null) {
            return;
        }
        waitlistRedisQueueService.cacheQueuedUser(event.getActivityId(), event.getUserId(), entity.getQueueNo(), activity.getEndTime());
    }

    /**
     * 处理候补取消后的缓存清理。
     *
     * @param event 候补取消事件
     */
    public void handleWaitlistCanceled(WaitlistCanceledEvent event) {
        waitlistRedisQueueService.removeQueuedUser(event.getActivityId(), event.getUserId());
    }

    /**
     * 在正式席位释放后尝试推进下一位候补用户。
     *
     * @param event 正式席位释放事件
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleFormalSeatReleased(FormalSignupCanceledEvent event) {
        WaitlistPromotionSourceEnum promotionSource = "候补补位确认超时".equals(event.getCancelReason())
                ? WaitlistPromotionSourceEnum.TIMEOUT_ROLLOVER
                : WaitlistPromotionSourceEnum.FORMAL_CANCEL;
        withPromotionLock(event.getActivityId(), () -> offerNextCandidate(event.getActivityId(), promotionSource));
    }

    /**
     * 扫描并处理已经超过确认时限的补位记录。
     */
    public void processExpiredConfirmations() {
        List<WaitlistTimeoutMember> timeoutMembers = waitlistRedisQueueService.listExpiredConfirmations(
                System.currentTimeMillis(),
                waitlistProperties.getTimeoutScanBatchSize());
        for (WaitlistTimeoutMember timeoutMember : timeoutMembers) {
            processExpiredConfirmation(timeoutMember);
        }
    }

    /**
     * 确认某个候补用户已经接受补位机会。
     * <p>
     * 当前阶段先把确认逻辑落在领域服务中，后续如果需要对外暴露接口，只需在控制层补一个入口。
     * </p>
     *
     * @param activityId 活动 ID
     * @param userId 用户 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirmPromotion(Long activityId, Long userId) {
        withPromotionLock(activityId, () -> {
            ActivityWaitlistEntity entity = activityWaitlistMapper.selectByActivityIdAndUserId(activityId, userId);
            if (entity == null || !Objects.equals(entity.getStatus(), WaitlistStatusEnum.WAITING_CONFIRM.getCode())) {
                return;
            }
            entity.setStatus(WaitlistStatusEnum.PROMOTED.getCode());
            entity.setConfirmedAt(LocalDateTime.now());
            activityWaitlistMapper.updateById(entity);
            waitlistRedisQueueService.clearPendingConfirm(activityId, userId);
        });
    }

    /**
     * 处理一条待超时确认记录。
     *
     * @param timeoutMember 超时索引成员
     */
    @Transactional(rollbackFor = Exception.class)
    public void processExpiredConfirmation(WaitlistTimeoutMember timeoutMember) {
        withPromotionLock(timeoutMember.getActivityId(), () -> {
            ActivityWaitlistEntity entity = activityWaitlistMapper.selectByActivityIdAndUserId(timeoutMember.getActivityId(), timeoutMember.getUserId());
            if (entity == null) {
                waitlistRedisQueueService.clearPendingConfirm(timeoutMember.getActivityId(), timeoutMember.getUserId());
                return;
            }
            if (!Objects.equals(entity.getStatus(), WaitlistStatusEnum.WAITING_CONFIRM.getCode())) {
                waitlistRedisQueueService.clearPendingConfirm(timeoutMember.getActivityId(), timeoutMember.getUserId());
                return;
            }
            if (entity.getConfirmDeadline() != null && entity.getConfirmDeadline().isAfter(LocalDateTime.now())) {
                return;
            }
            entity.setStatus(WaitlistStatusEnum.EXPIRED.getCode());
            entity.setExpiredAt(LocalDateTime.now());
            activityWaitlistMapper.updateById(entity);
            waitlistRedisQueueService.clearPendingConfirm(timeoutMember.getActivityId(), timeoutMember.getUserId());
            domainEventPublisher.publish(WaitlistPromotionExpiredEvent.builder()
                    .eventId(newEventId())
                    .activityId(entity.getActivityId())
                    .userId(entity.getUserId())
                    .queueNo(entity.getQueueNo())
                    .occurredAt(LocalDateTime.now())
                    .build());
        });
    }

    /**
     * 在持有活动级补位锁的前提下，推进下一位有效候补用户。
     *
     * @param activityId 活动 ID
     * @param promotionSource 补位来源
     */
    private void offerNextCandidate(Long activityId, WaitlistPromotionSourceEnum promotionSource) {
        ActivityEntity activity = activityMapper.selectById(activityId);
        if (activity == null || safeInt(activity.getAllowWaitlist()) != 1) {
            return;
        }
        while (true) {
            WaitlistQueueMember queueMember = waitlistRedisQueueService.peekFirst(activityId);
            if (queueMember == null) {
                return;
            }
            ActivityWaitlistEntity entity = activityWaitlistMapper.selectByActivityIdAndUserId(activityId, queueMember.getUserId());
            if (entity == null || !Objects.equals(entity.getStatus(), WaitlistStatusEnum.QUEUED.getCode())) {
                waitlistRedisQueueService.removeQueuedUser(activityId, queueMember.getUserId());
                continue;
            }
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime confirmDeadline = now.plusMinutes(waitlistProperties.getConfirmMinutes());
            entity.setStatus(WaitlistStatusEnum.WAITING_CONFIRM.getCode());
            entity.setPromotedAt(now);
            entity.setConfirmDeadline(confirmDeadline);
            entity.setPromotionSource(promotionSource.getCode());
            activityWaitlistMapper.updateById(entity);

            waitlistRedisQueueService.markWaitingConfirm(activityId, entity.getUserId(), confirmDeadline, activity.getEndTime());
            activity.setWaitlistCount(Math.max(0, safeInt(activity.getWaitlistCount()) - 1));
            refreshActivityStatusAfterWaitlistChange(activity);
            activityMapper.updateById(activity);
            notifyMessageMapper.insert(buildPromotionNotification(activity, entity, confirmDeadline));

            domainEventPublisher.publish(WaitlistPromotionOfferedEvent.builder()
                    .eventId(newEventId())
                    .activityId(entity.getActivityId())
                    .userId(entity.getUserId())
                    .queueNo(entity.getQueueNo())
                    .promotionSource(promotionSource.getCode())
                    .confirmDeadline(confirmDeadline)
                    .occurredAt(now)
                    .build());
            return;
        }
    }

    /**
     * 基于 Redisson 锁执行某个活动的补位动作。
     *
     * @param activityId 活动 ID
     * @param task 需要在锁内执行的任务
     */
    private void withPromotionLock(Long activityId, Runnable task) {
        RLock lock = redissonClient.getLock(WaitlistRedisKeys.promotionLock(activityId));
        boolean locked = false;
        try {
            locked = lock.tryLock(0, 10, TimeUnit.SECONDS);
            if (!locked) {
                return;
            }
            task.run();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 构造候补补位通知消息。
     *
     * @param activity 活动实体
     * @param entity 候补实体
     * @param confirmDeadline 确认截止时间
     * @return 通知消息实体
     */
    private NotifyMessageEntity buildPromotionNotification(ActivityEntity activity,
                                                           ActivityWaitlistEntity entity,
                                                           LocalDateTime confirmDeadline) {
        NotifyMessageEntity notifyMessage = new NotifyMessageEntity();
        notifyMessage.setUserId(entity.getUserId());
        notifyMessage.setMessageType(NotifyMessageTypeEnum.WAITLIST_PROMOTION.getCode());
        notifyMessage.setChannel(NotifyChannelEnum.IN_APP.getCode());
        notifyMessage.setTitle("候补补位提醒");
        notifyMessage.setContent("你已获得活动《" + activity.getTitle() + "》的补位机会，请在 " + confirmDeadline + " 前确认。");
        notifyMessage.setBizType("WAITLIST_PROMOTION");
        notifyMessage.setBizId(entity.getActivityId());
        notifyMessage.setStatus(NotifyStatusEnum.SENT.getCode());
        notifyMessage.setSendTime(LocalDateTime.now());
        return notifyMessage;
    }

    /**
     * 在候补人数变化后刷新活动状态。
     *
     * @param activity 活动实体
     */
    private void refreshActivityStatusAfterWaitlistChange(ActivityEntity activity) {
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
        activity.setStatus(waitlistCount > 0 ? ActivityStatusEnum.WAITLIST_OPEN.getCode() : ActivityStatusEnum.SIGNUP_OPEN.getCode());
    }

    /**
     * 把可空整数安全转成非空值。
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

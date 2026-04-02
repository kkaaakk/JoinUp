package com.joinup.waitlist.domain;

import com.joinup.activity.entity.ActivityEntity;
import com.joinup.activity.mapper.ActivityMapper;
import com.joinup.waitlist.entity.ActivityWaitlistEntity;
import com.joinup.waitlist.enums.WaitlistStatusEnum;
import com.joinup.waitlist.mapper.ActivityWaitlistMapper;
import com.joinup.waitlist.support.WaitlistQueueMember;
import com.joinup.waitlist.support.WaitlistRedisKeys;
import com.joinup.waitlist.support.WaitlistTimeoutMember;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 候补 Redis 队列服务。
 * <p>
 * 该服务专门负责维护候补队列在 Redis 中的结构，包括：
 * 1. 活动级别的候补顺序队列；
 * 2. 全局补位确认超时索引；
 * 3. 候补状态的轻量缓存快照。
 * </p>
 */
@Component
public class WaitlistRedisQueueService {

    private static final long FALLBACK_TTL_MILLIS = Duration.ofHours(24).toMillis();

    private final StringRedisTemplate stringRedisTemplate;
    private final ActivityWaitlistMapper activityWaitlistMapper;
    private final ActivityMapper activityMapper;

    /**
     * 构造候补 Redis 队列服务。
     *
     * @param stringRedisTemplate Redis 模板
     * @param activityWaitlistMapper 候补表访问器
     * @param activityMapper 活动表访问器
     */
    public WaitlistRedisQueueService(StringRedisTemplate stringRedisTemplate,
                                     ActivityWaitlistMapper activityWaitlistMapper,
                                     ActivityMapper activityMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.activityWaitlistMapper = activityWaitlistMapper;
        this.activityMapper = activityMapper;
    }

    /**
     * 把候补用户写入 Redis 队列。
     *
     * @param activityId 活动 ID
     * @param userId 用户 ID
     * @param queueNo 候补顺序号
     * @param activityEndTime 活动结束时间
     */
    public void cacheQueuedUser(Long activityId, Long userId, Integer queueNo, LocalDateTime activityEndTime) {
        long ttlMillis = resolveActivityTtlMillis(activityEndTime);
        String queueKey = WaitlistRedisKeys.queue(activityId);
        String stateKey = WaitlistRedisKeys.state(activityId);
        stringRedisTemplate.opsForZSet().add(queueKey, String.valueOf(userId), queueNo == null ? 0 : queueNo);
        stringRedisTemplate.opsForHash().put(stateKey, String.valueOf(userId), WaitlistStatusEnum.QUEUED.name());
        expireActivityScopedKeys(activityId, ttlMillis);
    }

    /**
     * 从 Redis 队列中移除某个候补用户。
     *
     * @param activityId 活动 ID
     * @param userId 用户 ID
     */
    public void removeQueuedUser(Long activityId, Long userId) {
        stringRedisTemplate.opsForZSet().remove(WaitlistRedisKeys.queue(activityId), String.valueOf(userId));
        stringRedisTemplate.opsForHash().delete(WaitlistRedisKeys.state(activityId), String.valueOf(userId));
        stringRedisTemplate.opsForZSet().remove(WaitlistRedisKeys.confirmTimeoutIndex(), WaitlistTimeoutMember.builder()
                .activityId(activityId)
                .userId(userId)
                .build()
                .serialize());
    }

    /**
     * 把某个候补用户标记为“已发放补位待确认”。
     *
     * @param activityId 活动 ID
     * @param userId 用户 ID
     * @param confirmDeadline 确认截止时间
     * @param activityEndTime 活动结束时间
     */
    public void markWaitingConfirm(Long activityId, Long userId, LocalDateTime confirmDeadline, LocalDateTime activityEndTime) {
        long ttlMillis = resolveActivityTtlMillis(activityEndTime);
        String serializedMember = WaitlistTimeoutMember.builder()
                .activityId(activityId)
                .userId(userId)
                .build()
                .serialize();
        stringRedisTemplate.opsForZSet().remove(WaitlistRedisKeys.queue(activityId), String.valueOf(userId));
        stringRedisTemplate.opsForHash().put(WaitlistRedisKeys.state(activityId), String.valueOf(userId), WaitlistStatusEnum.WAITING_CONFIRM.name());
        stringRedisTemplate.opsForZSet().add(WaitlistRedisKeys.confirmTimeoutIndex(), serializedMember, toEpochMilli(confirmDeadline));
        expireActivityScopedKeys(activityId, ttlMillis);
    }

    /**
     * 清理某个候补用户的待确认索引。
     *
     * @param activityId 活动 ID
     * @param userId 用户 ID
     */
    public void clearPendingConfirm(Long activityId, Long userId) {
        stringRedisTemplate.opsForZSet().remove(WaitlistRedisKeys.confirmTimeoutIndex(), WaitlistTimeoutMember.builder()
                .activityId(activityId)
                .userId(userId)
                .build()
                .serialize());
        stringRedisTemplate.opsForHash().delete(WaitlistRedisKeys.state(activityId), String.valueOf(userId));
    }

    /**
     * 查询当前用户在候补队列中的实时排名。
     *
     * @param activityId 活动 ID
     * @param userId 用户 ID
     * @return 1 开始的排名；如果当前不在排队中则返回 {@code null}
     */
    public Integer getRank(Long activityId, Long userId) {
        warmupQueueIfNecessary(activityId);
        Long rank = stringRedisTemplate.opsForZSet().rank(WaitlistRedisKeys.queue(activityId), String.valueOf(userId));
        return rank == null ? null : rank.intValue() + 1;
    }

    /**
     * 查询某个活动队头的一批候补成员。
     *
     * @param activityId 活动 ID
     * @param limit 查询条数
     * @return 候补成员列表
     */
    public List<WaitlistQueueMember> listHead(Long activityId, int limit) {
        warmupQueueIfNecessary(activityId);
        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                .rangeWithScores(WaitlistRedisKeys.queue(activityId), 0, Math.max(0, limit - 1));
        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }
        List<WaitlistQueueMember> result = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            WaitlistQueueMember member = WaitlistQueueMember.fromTuple(tuple);
            if (member != null) {
                result.add(member);
            }
        }
        return result;
    }

    /**
     * 读取当前活动的队头成员。
     *
     * @param activityId 活动 ID
     * @return 队头成员；若队列为空则返回 {@code null}
     */
    public WaitlistQueueMember peekFirst(Long activityId) {
        List<WaitlistQueueMember> members = listHead(activityId, 1);
        return members.isEmpty() ? null : members.get(0);
    }

    /**
     * 查询全局已经到期的补位待确认成员。
     *
     * @param nowEpochMillis 当前时间戳
     * @param limit 查询条数
     * @return 超时成员列表
     */
    public List<WaitlistTimeoutMember> listExpiredConfirmations(long nowEpochMillis, int limit) {
        Set<String> members = stringRedisTemplate.opsForZSet()
                .rangeByScore(WaitlistRedisKeys.confirmTimeoutIndex(), 0, nowEpochMillis, 0, limit);
        if (members == null || members.isEmpty()) {
            return Collections.emptyList();
        }
        List<WaitlistTimeoutMember> result = new ArrayList<>();
        for (String member : members) {
            WaitlistTimeoutMember timeoutMember = WaitlistTimeoutMember.parse(member);
            if (timeoutMember != null) {
                result.add(timeoutMember);
            }
        }
        return result;
    }

    /**
     * 在缓存缺失时从数据库预热候补队列。
     *
     * @param activityId 活动 ID
     */
    public void warmupQueueIfNecessary(Long activityId) {
        String queueKey = WaitlistRedisKeys.queue(activityId);
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(queueKey))) {
            return;
        }
        ActivityEntity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            return;
        }
        List<ActivityWaitlistEntity> queuedList = activityWaitlistMapper.selectTopByActivityIdAndStatus(
                activityId,
                WaitlistStatusEnum.QUEUED.getCode(),
                Integer.MAX_VALUE);
        long ttlMillis = resolveActivityTtlMillis(activity.getEndTime());
        for (ActivityWaitlistEntity queued : queuedList) {
            stringRedisTemplate.opsForZSet().add(queueKey, String.valueOf(queued.getUserId()), queued.getQueueNo());
            stringRedisTemplate.opsForHash().put(WaitlistRedisKeys.state(activityId), String.valueOf(queued.getUserId()), WaitlistStatusEnum.QUEUED.name());
        }
        expireActivityScopedKeys(activityId, ttlMillis);
    }

    /**
     * 为活动级别的 Redis Key 统一设置有效期。
     *
     * @param activityId 活动 ID
     * @param ttlMillis 有效期毫秒数
     */
    private void expireActivityScopedKeys(Long activityId, long ttlMillis) {
        stringRedisTemplate.expire(WaitlistRedisKeys.queue(activityId), ttlMillis, TimeUnit.MILLISECONDS);
        stringRedisTemplate.expire(WaitlistRedisKeys.state(activityId), ttlMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 计算活动级缓存 TTL。
     *
     * @param activityEndTime 活动结束时间
     * @return 缓存有效期，单位毫秒
     */
    private long resolveActivityTtlMillis(LocalDateTime activityEndTime) {
        if (activityEndTime == null) {
            return FALLBACK_TTL_MILLIS;
        }
        long ttlMillis = toEpochMilli(activityEndTime) - System.currentTimeMillis() + Duration.ofDays(1).toMillis();
        return Math.max(ttlMillis, FALLBACK_TTL_MILLIS);
    }

    /**
     * 把本地时间转换成毫秒时间戳。
     *
     * @param value 本地时间
     * @return 毫秒时间戳；为空时返回 0
     */
    private long toEpochMilli(LocalDateTime value) {
        if (value == null) {
            return 0L;
        }
        return value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}

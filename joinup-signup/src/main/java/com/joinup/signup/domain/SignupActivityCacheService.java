package com.joinup.signup.domain;

import com.joinup.activity.entity.ActivityEntity;
import com.joinup.activity.mapper.ActivityMapper;
import com.joinup.common.exception.BusinessException;
import com.joinup.common.exception.ErrorCode;
import com.joinup.signup.enums.SignupReservationTypeEnum;
import com.joinup.signup.enums.SignupResultStatusEnum;
import com.joinup.signup.event.SignupCommandEvent;
import com.joinup.signup.support.SignupActivitySnapshot;
import com.joinup.signup.support.SignupCachedState;
import com.joinup.signup.support.SignupRedisKeys;
import com.joinup.signup.support.SignupReservationResult;
import com.joinup.waitlist.mapper.ActivityWaitlistMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 报名活动缓存领域服务。
 * <p>
 * 该类统一管理报名高并发链路在 Redis 中使用的状态，包括活动快照、剩余名额、候补信息、
 * 用户幂等状态、请求结果以及失败补偿逻辑。
 * </p>
 * <p>
 * 可以把它理解为报名模块的“Redis 状态协调中心”。
 * </p>
 */
@Component
public class SignupActivityCacheService {

    /**
     * 活动缓存的最小兜底存活时长。
     * <p>
     * 即使活动已经结束，也额外保留至少一天缓存，给延迟消费者、失败补偿和结果查询留出时间窗口。
     * </p>
     */
    private static final long FALLBACK_TTL_MILLIS = Duration.ofHours(24).toMillis();

    /**
     * 请求结果类 Key 的默认存活时长。
     * <p>
     * 这类缓存主要用于短时间内的查询反馈和重复点击幂等控制，不需要像活动快照那样长期保留。
     * </p>
     */
    private static final long RESULT_TTL_MILLIS = Duration.ofHours(12).toMillis();

    private final StringRedisTemplate stringRedisTemplate;
    private final ActivityMapper activityMapper;
    private final ActivityWaitlistMapper activityWaitlistMapper;
    private final DefaultRedisScript<String> signupApplyScript;
    private final DefaultRedisScript<Long> signupCompensateScript;

    /**
     * 构造报名缓存领域服务，并初始化 Lua 脚本。
     *
     * @param stringRedisTemplate Redis 模板
     * @param activityMapper 活动表访问器
     * @param activityWaitlistMapper 候补表访问器
     */
    public SignupActivityCacheService(StringRedisTemplate stringRedisTemplate,
                                      ActivityMapper activityMapper,
                                      ActivityWaitlistMapper activityWaitlistMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.activityMapper = activityMapper;
        this.activityWaitlistMapper = activityWaitlistMapper;
        this.signupApplyScript = new DefaultRedisScript<>();
        this.signupApplyScript.setLocation(new ClassPathResource("lua/signup_apply.lua"));
        this.signupApplyScript.setResultType(String.class);
        this.signupCompensateScript = new DefaultRedisScript<>();
        this.signupCompensateScript.setLocation(new ClassPathResource("lua/signup_compensate.lua"));
        this.signupCompensateScript.setResultType(Long.class);
    }

    /**
     * 执行报名预占。
     * <p>
     * 这是高并发报名入口中最核心的同步步骤，主要职责有三件事：
     * 1. 确保活动快照已加载到 Redis；
     * 2. 调用 Lua 脚本原子完成重复报名判断与库存预占；
     * 3. 把 Lua 原始返回值转换为上层可直接使用的业务结果。
     * </p>
     *
     * @param activityId 活动 ID
     * @param userId 用户 ID
     * @param requestId 本次请求号
     * @return 预占结果对象
     */
    public SignupReservationResult reserve(Long activityId, Long userId, String requestId) {
        SignupActivitySnapshot snapshot = ensureActivityCache(activityId);
        if (snapshot == null) {
            throw new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND);
        }
        long ttlMillis = resolveActivityTtlMillis(snapshot.getEndTimeEpochMillis());
        String rawResult = executeApplyScript(activityId, userId, requestId, ttlMillis);
        if ("META_MISSING".equals(rawResult)) {
            // 说明当前线程拿到的活动元数据不存在，可能是别的线程刚好清理了缓存，
            // 也可能是首次预热尚未完成。这里主动重建一次并重试，避免把短暂抖动直接暴露给用户。
            evictActivityCache(activityId);
            snapshot = ensureActivityCache(activityId);
            rawResult = executeApplyScript(activityId, userId, requestId, ttlMillis);
        }
        return parseReservationResult(activityId, userId, rawResult);
    }

    /**
     * 读取用户在某个活动上的结果缓存。
     *
     * @param activityId 活动 ID
     * @param userId 用户 ID
     * @return 用户结果缓存；不存在时返回 {@code null}
     */
    public SignupCachedState getUserActivityResult(Long activityId, Long userId) {
        return SignupCachedState.parse(stringRedisTemplate.opsForValue().get(SignupRedisKeys.userResult(activityId, userId)));
    }

    /**
     * 读取用户在某个活动上的幂等状态缓存。
     *
     * @param activityId 活动 ID
     * @param userId 用户 ID
     * @return 用户幂等状态；不存在时返回 {@code null}
     */
    public SignupCachedState getUserState(Long activityId, Long userId) {
        return SignupCachedState.parse(stringRedisTemplate.opsForValue().get(SignupRedisKeys.userState(activityId, userId)));
    }

    /**
     * 标记取消报名命令已经进入异步处理队列。
     * <p>
     * 取消报名虽然不需要像报名那样抢占库存，但前端仍然需要看到“处理中”状态，
     * 这样用户不会误以为取消请求没有被系统接收。
     * </p>
     *
     * @param event 取消报名命令事件
     */
    public void markCancelProcessing(SignupCommandEvent event) {
        SignupCachedState currentState = getUserState(event.getActivityId(), event.getUserId());
        int queueNo = currentState == null || currentState.getQueueNo() == null ? 0 : currentState.getQueueNo();
        SignupCachedState processing = SignupCachedState.builder()
                .status(SignupResultStatusEnum.PROCESSING)
                .reservationType(event.getReservationType())
                .requestId(event.getRequestId())
                .queueNo(queueNo)
                .build();
        writeResultKeys(event.getActivityId(), event.getUserId(), event.getRequestId(), processing, RESULT_TTL_MILLIS);
    }

    /**
     * 标记报名申请已成功落库。
     * <p>
     * 当数据库写入成功后，需要把 Redis 中临时的 {@code PROCESSING} 状态覆盖为最终结果，
     * 并刷新活动快照，保证缓存状态和数据库尽可能快速收敛。
     * </p>
     *
     * @param event 报名命令事件
     * @param activity 最新活动实体
     */
    public void markApplySuccess(SignupCommandEvent event, ActivityEntity activity) {
        SignupCachedState finalState = SignupCachedState.builder()
                .status(event.getReservationType() == SignupReservationTypeEnum.FORMAL
                        ? SignupResultStatusEnum.SIGNED
                        : SignupResultStatusEnum.WAITLISTED)
                .reservationType(event.getReservationType())
                .requestId(event.getRequestId())
                .queueNo(event.getQueueNo())
                .build();
        long ttlMillis = resolveActivityTtlMillis(toEpochMilli(activity.getEndTime()));
        writeUserState(event.getActivityId(), event.getUserId(), finalState, ttlMillis);
        writeResultKeys(event.getActivityId(), event.getUserId(), event.getRequestId(), finalState, ttlMillis);
        updateActivityMeta(activity);
    }

    /**
     * 标记取消报名成功，并同步回补 Redis 名额。
     * <p>
     * 如果数据库层已经取消成功，而 Redis 中的剩余名额没有加回去，
     * 那么后续报名请求会错误地认为库存不足，因此这里必须同步回补缓存。
     * </p>
     *
     * @param event 取消命令事件
     * @param activity 最新活动实体
     */
    public void markCancelSuccess(SignupCommandEvent event, ActivityEntity activity) {
        if (event.getReservationType() == SignupReservationTypeEnum.FORMAL) {
            if (safeInt(activity.getWaitlistCount()) == 0) {
                safeIncrement(SignupRedisKeys.formalRemain(event.getActivityId()));
            }
        } else {
            safeIncrement(SignupRedisKeys.waitlistRemain(event.getActivityId()));
        }
        stringRedisTemplate.delete(SignupRedisKeys.userState(event.getActivityId(), event.getUserId()));
        SignupCachedState canceledState = SignupCachedState.builder()
                .status(SignupResultStatusEnum.CANCELED)
                .reservationType(event.getReservationType())
                .requestId(event.getRequestId())
                .queueNo(event.getQueueNo())
                .build();
        writeResultKeys(event.getActivityId(), event.getUserId(), event.getRequestId(), canceledState, RESULT_TTL_MILLIS);
        updateActivityMeta(activity);
    }

    /**
     * 标记候补补位已经重新占用了正式席位。
     * <p>
     * 正式用户取消后，Redis 中的正式余量会先被回补一次；
     * 如果候补模块马上为下一位用户发放补位机会，这里需要把那一个正式席位再次扣回去，
     * 防止新的直接报名请求把这一个已经分配给候补用户的席位再次抢走。
     * </p>
     *
     * @param activityId 活动 ID
     * @param userId 用户 ID
     * @param activity 最新活动实体
     */
    public void markPromotionReserved(Long activityId, Long userId, ActivityEntity activity) {
        safeConsumeFormalRemain(SignupRedisKeys.formalRemain(activityId));
        SignupCachedState state = SignupCachedState.builder()
                .status(SignupResultStatusEnum.SIGNED)
                .reservationType(SignupReservationTypeEnum.FORMAL)
                .queueNo(0)
                .build();
        long ttlMillis = resolveActivityTtlMillis(toEpochMilli(activity.getEndTime()));
        writeUserState(activityId, userId, state, ttlMillis);
        stringRedisTemplate.opsForValue().set(
                SignupRedisKeys.userResult(activityId, userId),
                state.serialize(),
                ttlMillis,
                TimeUnit.MILLISECONDS);
        updateActivityMeta(activity);
    }

    /**
     * 标记候补补位已经超时失效。
     * <p>
     * 候补补位超时后，需要把之前为该用户预留的正式席位归还给 Redis，
     * 同时清理用户状态缓存，避免后续查询误以为该用户仍然占有席位。
     * </p>
     *
     * @param activityId 活动 ID
     * @param userId 用户 ID
     * @param activity 最新活动实体
     */
    public void markPromotionExpired(Long activityId, Long userId, ActivityEntity activity) {
        if (safeInt(activity.getWaitlistCount()) == 0) {
            safeIncrement(SignupRedisKeys.formalRemain(activityId));
        }
        stringRedisTemplate.delete(SignupRedisKeys.userState(activityId, userId));
        stringRedisTemplate.delete(SignupRedisKeys.userResult(activityId, userId));
        updateActivityMeta(activity);
    }

    /**
     * 对报名申请失败进行缓存补偿。
     * <p>
     * 该方法用于处理“Redis 已预占、但数据库最终未成功落库”的场景，
     * 需要把之前扣掉的正式名额或候补容量归还，并把结果标记为失败。
     * </p>
     *
     * @param event 失败的报名命令事件
     */
    public void compensateFailedApply(SignupCommandEvent event) {
        List<String> keys = List.of(
                SignupRedisKeys.formalRemain(event.getActivityId()),
                SignupRedisKeys.waitlistRemain(event.getActivityId()),
                SignupRedisKeys.userState(event.getActivityId(), event.getUserId()),
                SignupRedisKeys.userResult(event.getActivityId(), event.getUserId()),
                SignupRedisKeys.requestResult(event.getRequestId()));
        stringRedisTemplate.execute(
                signupCompensateScript,
                keys,
                event.getReservationType().getCode(),
                event.getRequestId(),
                String.valueOf(RESULT_TTL_MILLIS));
    }

    /**
     * 标记取消报名失败。
     * <p>
     * 取消失败通常不需要像报名失败那样回滚库存，因为取消流程本身不会额外占用库存。
     * 这里主要是把失败结果暴露给调用方，让其感知当前状态未发生改变。
     * </p>
     *
     * @param event 失败的取消命令事件
     */
    public void markCancelFailed(SignupCommandEvent event) {
        SignupCachedState failedState = SignupCachedState.builder()
                .status(SignupResultStatusEnum.FAILED)
                .reservationType(event.getReservationType())
                .requestId(event.getRequestId())
                .queueNo(event.getQueueNo())
                .build();
        writeResultKeys(event.getActivityId(), event.getUserId(), event.getRequestId(), failedState, RESULT_TTL_MILLIS);
    }

    /**
     * 刷新活动元数据缓存。
     * <p>
     * Lua 脚本依赖的活动状态、是否允许候补、报名截止时间等信息都在这里同步更新。
     * 其中正式余量和候补余量仍然保存在单独字符串 Key 中，因为它们更新更频繁。
     * </p>
     *
     * @param activity 最新活动实体
     */
    public void updateActivityMeta(ActivityEntity activity) {
        Map<Object, Object> meta = new HashMap<>();
        meta.put("status", String.valueOf(activity.getStatus()));
        meta.put("allowWaitlist", String.valueOf(activity.getAllowWaitlist()));
        meta.put("signupDeadline", String.valueOf(toEpochMilli(activity.getSignupDeadline())));
        meta.put("endTime", String.valueOf(toEpochMilli(activity.getEndTime())));
        String metaKey = SignupRedisKeys.activityMeta(activity.getId());
        stringRedisTemplate.opsForHash().putAll(metaKey, meta);
        stringRedisTemplate.expire(metaKey, resolveActivityTtl(toEpochMilli(activity.getEndTime())));
    }

    /**
     * 确保活动快照已经加载到 Redis。
     * <p>
     * 如果缓存不存在，则会回源数据库读取活动与候补信息并完成一次懒加载。
     * 这样热门活动在首次预热后，后续报名请求就不需要重复触达数据库。
     * </p>
     *
     * @param activityId 活动 ID
     * @return 活动快照；如果活动不存在则返回 {@code null}
     */
    private SignupActivitySnapshot ensureActivityCache(Long activityId) {
        SignupActivitySnapshot cached = readActivitySnapshot(activityId);
        if (cached != null) {
            return cached;
        }
        ActivityEntity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            return null;
        }
        Integer maxQueueNo = activityWaitlistMapper.selectMaxQueueNo(activityId);
        SignupActivitySnapshot snapshot = SignupActivitySnapshot.builder()
                .activityId(activityId)
                .status(activity.getStatus())
                .allowWaitlist(activity.getAllowWaitlist())
                .formalRemaining(Math.max(0, safeInt(activity.getMaxParticipants()) - safeInt(activity.getCurrentParticipants())))
                .waitlistRemaining(activity.getAllowWaitlist() != null && activity.getAllowWaitlist() == 1
                        ? Math.max(0, safeInt(activity.getWaitlistLimit()) - safeInt(activity.getWaitlistCount()))
                        : 0)
                .maxQueueNo(maxQueueNo == null ? 0 : maxQueueNo)
                .signupDeadlineEpochMillis(toEpochMilli(activity.getSignupDeadline()))
                .endTimeEpochMillis(toEpochMilli(activity.getEndTime()))
                .build();
        cacheSnapshot(snapshot);
        return snapshot;
    }

    /**
     * 读取 Redis 中的当前活动快照。
     * <p>
     * 如果元数据 Hash 不存在，说明快照尚未初始化或已经过期，此时返回 {@code null}。
     * </p>
     *
     * @param activityId 活动 ID
     * @return 活动快照；若缓存未命中则返回 {@code null}
     */
    private SignupActivitySnapshot readActivitySnapshot(Long activityId) {
        Map<Object, Object> meta = stringRedisTemplate.opsForHash().entries(SignupRedisKeys.activityMeta(activityId));
        if (CollectionUtils.isEmpty(meta)) {
            return null;
        }
        String formalRemain = stringRedisTemplate.opsForValue().get(SignupRedisKeys.formalRemain(activityId));
        String waitlistRemain = stringRedisTemplate.opsForValue().get(SignupRedisKeys.waitlistRemain(activityId));
        String queueSeq = stringRedisTemplate.opsForValue().get(SignupRedisKeys.waitlistQueueSeq(activityId));
        return SignupActivitySnapshot.builder()
                .activityId(activityId)
                .status(parseInteger(meta.get("status")))
                .allowWaitlist(parseInteger(meta.get("allowWaitlist")))
                .formalRemaining(parseInteger(formalRemain))
                .waitlistRemaining(parseInteger(waitlistRemain))
                .maxQueueNo(parseInteger(queueSeq))
                .signupDeadlineEpochMillis(parseLong(meta.get("signupDeadline")))
                .endTimeEpochMillis(parseLong(meta.get("endTime")))
                .build();
    }

    /**
     * 把活动快照写入 Redis。
     * <p>
     * 这里使用 {@code setIfAbsent} 进行幂等预热，避免多个线程同时初始化同一活动缓存时互相覆盖。
     * </p>
     *
     * @param snapshot 活动快照
     */
    private void cacheSnapshot(SignupActivitySnapshot snapshot) {
        Duration ttl = resolveActivityTtl(snapshot.getEndTimeEpochMillis());
        String activityMetaKey = SignupRedisKeys.activityMeta(snapshot.getActivityId());
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(activityMetaKey))) {
            return;
        }
        stringRedisTemplate.opsForValue().setIfAbsent(
                SignupRedisKeys.formalRemain(snapshot.getActivityId()),
                String.valueOf(snapshot.getFormalRemaining()),
                ttl);
        stringRedisTemplate.opsForValue().setIfAbsent(
                SignupRedisKeys.waitlistRemain(snapshot.getActivityId()),
                String.valueOf(snapshot.getWaitlistRemaining()),
                ttl);
        stringRedisTemplate.opsForValue().setIfAbsent(
                SignupRedisKeys.waitlistQueueSeq(snapshot.getActivityId()),
                String.valueOf(snapshot.getMaxQueueNo()),
                ttl);

        Map<String, String> meta = new HashMap<>();
        meta.put("status", String.valueOf(snapshot.getStatus()));
        meta.put("allowWaitlist", String.valueOf(snapshot.getAllowWaitlist()));
        meta.put("signupDeadline", String.valueOf(snapshot.getSignupDeadlineEpochMillis()));
        meta.put("endTime", String.valueOf(snapshot.getEndTimeEpochMillis()));
        stringRedisTemplate.opsForHash().putAll(activityMetaKey, meta);
        stringRedisTemplate.expire(activityMetaKey, ttl);
    }

    /**
     * 执行报名 Lua 脚本。
     * <p>
     * 所有与并发安全直接相关的判断都必须在 Lua 中一次性完成，
     * Java 侧这里只负责准备 Key 和参数，避免在 JVM 层出现非原子竞争判断。
     * </p>
     *
     * @param activityId 活动 ID
     * @param userId 用户 ID
     * @param requestId 请求号
     * @param ttlMillis 结果缓存有效期
     * @return Lua 返回的原始字符串结果
     */
    private String executeApplyScript(Long activityId, Long userId, String requestId, long ttlMillis) {
        return stringRedisTemplate.execute(
                signupApplyScript,
                List.of(
                        SignupRedisKeys.formalRemain(activityId),
                        SignupRedisKeys.waitlistRemain(activityId),
                        SignupRedisKeys.userState(activityId, userId),
                        SignupRedisKeys.userResult(activityId, userId),
                        SignupRedisKeys.requestResult(requestId),
                        SignupRedisKeys.activityMeta(activityId),
                        SignupRedisKeys.waitlistQueueSeq(activityId)),
                requestId,
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(ttlMillis));
    }

    /**
     * 解析 Lua 返回值并转换为业务对象。
     * <p>
     * 通过这一层转换，可以让上层服务不直接依赖脚本返回的字符串协议，
     * 从而把 Redis 脚本细节封装在缓存服务内部。
     * </p>
     *
     * @param activityId 活动 ID
     * @param userId 用户 ID
     * @param rawResult Lua 原始返回值
     * @return 报名预占结果对象
     */
    private SignupReservationResult parseReservationResult(Long activityId, Long userId, String rawResult) {
        if (!StringUtils.hasText(rawResult)) {
            throw new BusinessException(ErrorCode.SIGNUP_CACHE_INITIALIZATION_FAILED, "Empty Lua execution result");
        }
        if ("DUPLICATE".equals(rawResult)) {
            // 优先读取用户幂等状态，因为它的生命周期通常更长、语义也更稳定；
            // 如果该 Key 已经过期，再退回到短期结果 Key，尽量把已知状态返回给调用方。
            SignupCachedState currentState = getUserState(activityId, userId);
            if (currentState == null) {
                currentState = getUserActivityResult(activityId, userId);
            }
            return SignupReservationResult.builder()
                    .accepted(false)
                    .duplicateRequest(true)
                    .cachedState(currentState)
                    .reservationType(currentState == null ? null : currentState.getReservationType())
                    .queueNo(currentState == null ? null : currentState.getQueueNo())
                    .build();
        }
        if ("CLOSED".equals(rawResult)) {
            throw new BusinessException(ErrorCode.SIGNUP_ACTIVITY_NOT_OPEN);
        }
        if ("DEADLINE".equals(rawResult)) {
            throw new BusinessException(ErrorCode.SIGNUP_DEADLINE_EXPIRED);
        }
        if ("FULL".equals(rawResult)) {
            throw new BusinessException(ErrorCode.SIGNUP_FULL);
        }
        if ("WAITLIST_FULL".equals(rawResult)) {
            throw new BusinessException(ErrorCode.SIGNUP_WAITLIST_FULL);
        }
        if (rawResult.startsWith("FORMAL:")) {
            return SignupReservationResult.builder()
                    .accepted(true)
                    .duplicateRequest(false)
                    .reservationType(SignupReservationTypeEnum.FORMAL)
                    .queueNo(0)
                    .cachedState(SignupCachedState.builder()
                            .status(SignupResultStatusEnum.PROCESSING)
                            .reservationType(SignupReservationTypeEnum.FORMAL)
                            .build())
                    .build();
        }
        if (rawResult.startsWith("WAITLIST:")) {
            int queueNo = Integer.parseInt(rawResult.substring("WAITLIST:".length()));
            return SignupReservationResult.builder()
                    .accepted(true)
                    .duplicateRequest(false)
                    .reservationType(SignupReservationTypeEnum.WAITLIST)
                    .queueNo(queueNo)
                    .cachedState(SignupCachedState.builder()
                            .status(SignupResultStatusEnum.PROCESSING)
                            .reservationType(SignupReservationTypeEnum.WAITLIST)
                            .queueNo(queueNo)
                            .build())
                    .build();
        }
        throw new BusinessException(ErrorCode.SIGNUP_CACHE_INITIALIZATION_FAILED, "Unsupported Lua result: " + rawResult);
    }

    /**
     * 写入用户在某活动上的幂等状态。
     *
     * @param activityId 活动 ID
     * @param userId 用户 ID
     * @param state 状态对象
     * @param ttlMillis 有效期
     */
    private void writeUserState(Long activityId, Long userId, SignupCachedState state, long ttlMillis) {
        stringRedisTemplate.opsForValue().set(
                SignupRedisKeys.userState(activityId, userId),
                state.serialize(),
                ttlMillis,
                TimeUnit.MILLISECONDS);
    }

    /**
     * 同时写入用户结果 Key 与请求结果 Key。
     * <p>
     * 两者保存的是同一份业务状态，只是索引维度不同，分别面向“按用户查询”和“按请求查询”两种场景。
     * </p>
     *
     * @param activityId 活动 ID
     * @param userId 用户 ID
     * @param requestId 请求号
     * @param state 状态对象
     * @param ttlMillis 有效期
     */
    private void writeResultKeys(Long activityId, Long userId, String requestId, SignupCachedState state, long ttlMillis) {
        String value = state.serialize();
        stringRedisTemplate.opsForValue().set(SignupRedisKeys.userResult(activityId, userId), value, ttlMillis, TimeUnit.MILLISECONDS);
        stringRedisTemplate.opsForValue().set(SignupRedisKeys.requestResult(requestId), value, ttlMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 对已有数量型 Key 执行安全自增。
     * <p>
     * 只有在 Key 已存在的情况下才回补库存，避免因为缓存已失效而意外创建出孤立计数键。
     * </p>
     *
     * @param key 需要自增的 Redis Key
     */
    private void safeIncrement(String key) {
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
            stringRedisTemplate.opsForValue().increment(key);
        }
    }

    /**
     * 安全消费一份正式名额库存。
     * <p>
     * 只有当 Key 存在且当前剩余值大于 0 时才会真正扣减，避免把正式库存扣成负数。
     * 当席位本来就是为候补链路保留时，Redis 中的正式余量可能已经是 0，此时应保持不变。
     * </p>
     *
     * @param key 正式名额剩余值的 Redis Key
     */
    private void safeConsumeFormalRemain(String key) {
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
            String currentValue = stringRedisTemplate.opsForValue().get(key);
            long remain = currentValue == null ? 0L : Long.parseLong(currentValue);
            if (remain > 0) {
                stringRedisTemplate.opsForValue().increment(key, -1);
            }
        }
    }

    /**
     * 删除某个活动的报名缓存。
     * <p>
     * 当怀疑活动快照缺失、损坏或不一致时，可以先清理，再由后续请求重新回源构建。
     * </p>
     *
     * @param activityId 活动 ID
     */
    private void evictActivityCache(Long activityId) {
        stringRedisTemplate.delete(List.of(
                SignupRedisKeys.activityMeta(activityId),
                SignupRedisKeys.formalRemain(activityId),
                SignupRedisKeys.waitlistRemain(activityId),
                SignupRedisKeys.waitlistQueueSeq(activityId)));
    }

    /**
     * 计算活动缓存 TTL 对应的 {@link Duration}。
     *
     * @param endTimeEpochMillis 活动结束时间时间戳
     * @return 缓存时长对象
     */
    private Duration resolveActivityTtl(Long endTimeEpochMillis) {
        return Duration.ofMillis(resolveActivityTtlMillis(endTimeEpochMillis));
    }

    /**
     * 计算活动缓存的实际毫秒级 TTL。
     * <p>
     * 缓存会比活动结束时间多保留一段缓冲期，以覆盖延迟消费、补偿和查询等场景。
     * 如果结束时间不可用，则退化到兜底 TTL。
     * </p>
     *
     * @param endTimeEpochMillis 活动结束时间时间戳
     * @return 缓存有效期，单位毫秒
     */
    private long resolveActivityTtlMillis(Long endTimeEpochMillis) {
        if (endTimeEpochMillis == null || endTimeEpochMillis <= 0) {
            return FALLBACK_TTL_MILLIS;
        }
        long ttlMillis = endTimeEpochMillis - System.currentTimeMillis() + Duration.ofDays(1).toMillis();
        return Math.max(ttlMillis, FALLBACK_TTL_MILLIS);
    }

    /**
     * 把可空整数安全转为非空值。
     *
     * @param value 原始整数
     * @return 非空整数；为空时返回 0
     */
    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 把本地时间转换为毫秒时间戳。
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

    /**
     * 把 Redis 中读取到的值解析成整数。
     *
     * @param value Redis 原始值
     * @return 解析后的整数；为空时返回 0
     */
    private Integer parseInteger(Object value) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return 0;
        }
        return Integer.parseInt(String.valueOf(value));
    }

    /**
     * 把 Redis 中读取到的值解析成长整数。
     *
     * @param value Redis 原始值
     * @return 解析后的长整数；为空时返回 0
     */
    private Long parseLong(Object value) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return 0L;
        }
        return Long.parseLong(String.valueOf(value));
    }
}

package com.joinup.signup.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.joinup.activity.entity.ActivityEntity;
import com.joinup.activity.mapper.ActivityMapper;
import com.joinup.common.context.LoginUser;
import com.joinup.common.exception.BusinessException;
import com.joinup.common.exception.ErrorCode;
import com.joinup.signup.domain.SignupActivityCacheService;
import com.joinup.signup.entity.ActivitySignupEntity;
import com.joinup.signup.enums.SignupCommandTypeEnum;
import com.joinup.signup.enums.SignupReservationTypeEnum;
import com.joinup.signup.enums.SignupResultStatusEnum;
import com.joinup.signup.enums.SignupStatusEnum;
import com.joinup.signup.event.SignupCommandEvent;
import com.joinup.signup.mapper.ActivitySignupMapper;
import com.joinup.signup.producer.SignupCommandProducer;
import com.joinup.signup.service.SignupService;
import com.joinup.signup.support.SignupCachedState;
import com.joinup.signup.support.SignupReservationResult;
import com.joinup.signup.vo.SignupActivityResultVO;
import com.joinup.signup.vo.SignupApplyVO;
import com.joinup.signup.vo.SignupMyItemVO;
import com.joinup.waitlist.entity.ActivityWaitlistEntity;
import com.joinup.waitlist.enums.WaitlistStatusEnum;
import com.joinup.waitlist.mapper.ActivityWaitlistMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 报名应用服务实现。
 * <p>
 * 该类位于控制层和底层高并发能力之间，负责把一次报名请求编排成：
 * 1. 同步阶段的 Redis 预占；
 * 2. 异步阶段的 Kafka 投递与后续落库；
 * 3. 查询阶段对 Redis 与数据库结果的聚合。
 * </p>
 */
@Service
public class SignupServiceImpl implements SignupService {

    private static final int MAX_MY_RECORDS = 100;

    private final ActivityMapper activityMapper;
    private final ActivitySignupMapper activitySignupMapper;
    private final ActivityWaitlistMapper activityWaitlistMapper;
    private final SignupActivityCacheService signupActivityCacheService;
    private final SignupCommandProducer signupCommandProducer;

    /**
     * 构造报名应用服务。
     *
     * @param activityMapper 活动表访问器
     * @param activitySignupMapper 正式报名表访问器
     * @param activityWaitlistMapper 候补表访问器
     * @param signupActivityCacheService Redis 报名缓存服务
     * @param signupCommandProducer Kafka 报名命令生产者
     */
    public SignupServiceImpl(ActivityMapper activityMapper,
                             ActivitySignupMapper activitySignupMapper,
                             ActivityWaitlistMapper activityWaitlistMapper,
                             SignupActivityCacheService signupActivityCacheService,
                             SignupCommandProducer signupCommandProducer) {
        this.activityMapper = activityMapper;
        this.activitySignupMapper = activitySignupMapper;
        this.activityWaitlistMapper = activityWaitlistMapper;
        this.signupActivityCacheService = signupActivityCacheService;
        this.signupCommandProducer = signupCommandProducer;
    }

    /**
     * 申请报名指定活动。
     * <p>
     * 该方法是同步入口中的主流程：
     * 1. 校验当前登录用户；
     * 2. 生成本次请求号；
     * 3. 通过 Redis + Lua 执行原子预占；
     * 4. 若属于重复请求，直接返回缓存状态；
     * 5. 若预占成功，则发送 Kafka 命令等待异步落库；
     * 6. 如果消息投递失败，则立即执行 Redis 补偿，归还预占资源。
     * </p>
     *
     * @param activityId 活动 ID
     * @param loginUser 当前登录用户
     * @return 报名受理结果
     */
    @Override
    public SignupApplyVO apply(Long activityId, LoginUser loginUser) {
        Long userId = currentUserId(loginUser);
        String requestId = newRequestId();
        SignupReservationResult reservationResult = signupActivityCacheService.reserve(activityId, userId, requestId);
        if (reservationResult.isDuplicateRequest()) {
            return buildDuplicateApplyVO(activityId, reservationResult.getCachedState());
        }

        SignupCommandEvent event = SignupCommandEvent.builder()
                .requestId(requestId)
                .activityId(activityId)
                .userId(userId)
                .commandType(SignupCommandTypeEnum.APPLY)
                .reservationType(reservationResult.getReservationType())
                .queueNo(reservationResult.getQueueNo())
                .occurredAt(LocalDateTime.now())
                .build();

        try {
            signupCommandProducer.send(event);
        } catch (BusinessException ex) {
            signupActivityCacheService.compensateFailedApply(event);
            throw ex;
        }

        return SignupApplyVO.builder()
                .requestId(requestId)
                .activityId(activityId)
                .resultStatus(SignupResultStatusEnum.PROCESSING.getCode())
                .reservationType(reservationResult.getReservationType().getCode())
                .queueNo(reservationResult.getQueueNo())
                .duplicateRequest(Boolean.FALSE)
                .message(reservationResult.getReservationType() == SignupReservationTypeEnum.FORMAL
                        ? "Formal seat reserved, waiting for persistence"
                        : "Waitlist seat reserved, waiting for persistence")
                .build();
    }

    /**
     * 提交取消报名请求。
     * <p>
     * 当前实现不会在同步请求里直接写数据库，而是把取消命令投递到 Kafka，
     * 由异步消费者统一在事务中处理，避免高峰期接口线程直接参与数据库锁竞争。
     * </p>
     *
     * @param activityId 活动 ID
     * @param loginUser 当前登录用户
     * @param reason 取消原因，可为空
     * @return 取消受理结果
     */
    @Override
    public SignupApplyVO cancel(Long activityId, LoginUser loginUser, String reason) {
        Long userId = currentUserId(loginUser);
        SignupCachedState currentState = signupActivityCacheService.getUserState(activityId, userId);
        if (currentState != null && currentState.getStatus() == SignupResultStatusEnum.PROCESSING) {
            throw new BusinessException(ErrorCode.CONFLICT, "Signup request is still processing");
        }

        SignupReservationTypeEnum reservationType = resolveCancelableReservationType(activityId, userId);
        SignupCommandEvent event = SignupCommandEvent.builder()
                .requestId(newRequestId())
                .activityId(activityId)
                .userId(userId)
                .commandType(SignupCommandTypeEnum.CANCEL)
                .reservationType(reservationType)
                .queueNo(currentState == null ? 0 : currentState.getQueueNo())
                .cancelReason(StringUtils.hasText(reason) ? reason.trim() : "Canceled by user")
                .occurredAt(LocalDateTime.now())
                .build();

        signupCommandProducer.send(event);
        signupActivityCacheService.markCancelProcessing(event);
        return SignupApplyVO.builder()
                .requestId(event.getRequestId())
                .activityId(activityId)
                .resultStatus(SignupResultStatusEnum.PROCESSING.getCode())
                .reservationType(reservationType.getCode())
                .queueNo(event.getQueueNo())
                .duplicateRequest(Boolean.FALSE)
                .message("Cancel command accepted, waiting for persistence")
                .build();
    }

    /**
     * 查询当前用户最近的报名记录。
     * <p>
     * 这里会分别查询正式报名表和候补表，再统一映射为前端可直接消费的列表项。
     * 同时会批量回查活动标题，避免出现 N+1 查询。
     * </p>
     *
     * @param loginUser 当前登录用户
     * @return 当前用户的报名记录列表
     */
    @Override
    public List<SignupMyItemVO> listMySignups(LoginUser loginUser) {
        Long userId = currentUserId(loginUser);
        List<ActivitySignupEntity> signupEntities = activitySignupMapper.selectList(new LambdaQueryWrapper<ActivitySignupEntity>()
                .eq(ActivitySignupEntity::getUserId, userId)
                .orderByDesc(ActivitySignupEntity::getUpdatedAt)
                .last("limit " + MAX_MY_RECORDS));
        List<ActivityWaitlistEntity> waitlistEntities = activityWaitlistMapper.selectList(new LambdaQueryWrapper<ActivityWaitlistEntity>()
                .eq(ActivityWaitlistEntity::getUserId, userId)
                .orderByDesc(ActivityWaitlistEntity::getUpdatedAt)
                .last("limit " + MAX_MY_RECORDS));

        List<Long> activityIds = new ArrayList<>();
        signupEntities.forEach(item -> activityIds.add(item.getActivityId()));
        waitlistEntities.forEach(item -> activityIds.add(item.getActivityId()));
        Map<Long, ActivityEntity> activityMap = loadActivityMap(activityIds);

        List<SignupMyItemVO> result = new ArrayList<>();
        for (ActivitySignupEntity signupEntity : signupEntities) {
            result.add(SignupMyItemVO.builder()
                    .activityId(signupEntity.getActivityId())
                    .activityTitle(activityTitle(activityMap, signupEntity.getActivityId()))
                    .resultStatus(mapSignupResultStatus(signupEntity.getStatus()))
                    .reservationType(SignupReservationTypeEnum.FORMAL.getCode())
                    .signupStatus(String.valueOf(signupEntity.getStatus()))
                    .queueNo(0)
                    .signupTime(signupEntity.getSignupTime())
                    .cancelTime(signupEntity.getCancelTime())
                    .updatedAt(signupEntity.getUpdatedAt())
                    .build());
        }
        for (ActivityWaitlistEntity waitlistEntity : waitlistEntities) {
            result.add(SignupMyItemVO.builder()
                    .activityId(waitlistEntity.getActivityId())
                    .activityTitle(activityTitle(activityMap, waitlistEntity.getActivityId()))
                    .resultStatus(mapWaitlistResultStatus(waitlistEntity.getStatus()))
                    .reservationType(SignupReservationTypeEnum.WAITLIST.getCode())
                    .waitlistStatus(String.valueOf(waitlistEntity.getStatus()))
                    .queueNo(waitlistEntity.getQueueNo())
                    .signupTime(waitlistEntity.getJoinedAt())
                    .cancelTime(waitlistEntity.getExpiredAt())
                    .updatedAt(waitlistEntity.getUpdatedAt())
                    .build());
        }
        result.sort(Comparator.comparing(SignupMyItemVO::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        return result.size() > MAX_MY_RECORDS ? result.subList(0, MAX_MY_RECORDS) : result;
    }

    /**
     * 查询当前用户在指定活动上的报名结果。
     * <p>
     * 查询优先级是：
     * 1. 先看 Redis 中是否仍处于处理中；
     * 2. 再看正式报名表；
     * 3. 再看候补表；
     * 4. 若仍未命中，则兜底返回缓存终态或 NONE。
     * </p>
     *
     * @param activityId 活动 ID
     * @param loginUser 当前登录用户
     * @return 当前活动的报名结果
     */
    @Override
    public SignupActivityResultVO getCurrentUserActivityResult(Long activityId, LoginUser loginUser) {
        Long userId = currentUserId(loginUser);
        SignupCachedState cachedState = signupActivityCacheService.getUserActivityResult(activityId, userId);
        ActivityEntity activity = activityMapper.selectById(activityId);
        if (cachedState != null && cachedState.getStatus() == SignupResultStatusEnum.PROCESSING) {
            return SignupActivityResultVO.builder()
                    .activityId(activityId)
                    .activityTitle(activity == null ? null : activity.getTitle())
                    .userId(userId)
                    .requestId(cachedState.getRequestId())
                    .resultStatus(cachedState.getStatus().getCode())
                    .reservationType(cachedState.getReservationType() == null ? null : cachedState.getReservationType().getCode())
                    .queueNo(cachedState.getQueueNo())
                    .message("Result is still being persisted")
                    .updatedAt(LocalDateTime.now())
                    .build();
        }

        ActivitySignupEntity signupEntity = activitySignupMapper.selectOne(new LambdaQueryWrapper<ActivitySignupEntity>()
                .eq(ActivitySignupEntity::getActivityId, activityId)
                .eq(ActivitySignupEntity::getUserId, userId)
                .last("limit 1"));
        if (signupEntity != null) {
            return SignupActivityResultVO.builder()
                    .activityId(activityId)
                    .activityTitle(activity == null ? null : activity.getTitle())
                    .userId(userId)
                    .requestId(cachedState == null ? null : cachedState.getRequestId())
                    .resultStatus(mapSignupResultStatus(signupEntity.getStatus()))
                    .reservationType(SignupReservationTypeEnum.FORMAL.getCode())
                    .signupStatus(String.valueOf(signupEntity.getStatus()))
                    .queueNo(0)
                    .message(resultMessage(mapSignupResultStatus(signupEntity.getStatus())))
                    .updatedAt(signupEntity.getUpdatedAt())
                    .build();
        }

        ActivityWaitlistEntity waitlistEntity = activityWaitlistMapper.selectOne(new LambdaQueryWrapper<ActivityWaitlistEntity>()
                .eq(ActivityWaitlistEntity::getActivityId, activityId)
                .eq(ActivityWaitlistEntity::getUserId, userId)
                .last("limit 1"));
        if (waitlistEntity != null) {
            return SignupActivityResultVO.builder()
                    .activityId(activityId)
                    .activityTitle(activity == null ? null : activity.getTitle())
                    .userId(userId)
                    .requestId(cachedState == null ? null : cachedState.getRequestId())
                    .resultStatus(mapWaitlistResultStatus(waitlistEntity.getStatus()))
                    .reservationType(SignupReservationTypeEnum.WAITLIST.getCode())
                    .waitlistStatus(String.valueOf(waitlistEntity.getStatus()))
                    .queueNo(waitlistEntity.getQueueNo())
                    .message(resultMessage(mapWaitlistResultStatus(waitlistEntity.getStatus())))
                    .updatedAt(waitlistEntity.getUpdatedAt())
                    .build();
        }

        if (cachedState != null) {
            String resultStatus = cachedState.getStatus() == null ? SignupResultStatusEnum.NONE.getCode() : cachedState.getStatus().getCode();
            return SignupActivityResultVO.builder()
                    .activityId(activityId)
                    .activityTitle(activity == null ? null : activity.getTitle())
                    .userId(userId)
                    .requestId(cachedState.getRequestId())
                    .resultStatus(resultStatus)
                    .reservationType(cachedState.getReservationType() == null ? null : cachedState.getReservationType().getCode())
                    .queueNo(cachedState.getQueueNo())
                    .message(resultMessage(resultStatus))
                    .updatedAt(LocalDateTime.now())
                    .build();
        }

        return SignupActivityResultVO.builder()
                .activityId(activityId)
                .activityTitle(activity == null ? null : activity.getTitle())
                .userId(userId)
                .resultStatus(SignupResultStatusEnum.NONE.getCode())
                .message("No signup record found")
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 构造重复报名请求的返回对象。
     * <p>
     * 当 Redis 判断出该用户已经有相同活动的状态时，不会再次扣减名额，
     * 而是直接把当前缓存中的状态返回给前端。
     * </p>
     *
     * @param activityId 活动 ID
     * @param cachedState 当前缓存状态
     * @return 重复请求对应的响应对象
     */
    private SignupApplyVO buildDuplicateApplyVO(Long activityId, SignupCachedState cachedState) {
        if (cachedState == null) {
            return SignupApplyVO.builder()
                    .activityId(activityId)
                    .duplicateRequest(Boolean.TRUE)
                    .resultStatus(SignupResultStatusEnum.PROCESSING.getCode())
                    .message("Duplicate request detected")
                    .build();
        }
        return SignupApplyVO.builder()
                .requestId(cachedState.getRequestId())
                .activityId(activityId)
                .resultStatus(cachedState.getStatus() == null ? SignupResultStatusEnum.PROCESSING.getCode() : cachedState.getStatus().getCode())
                .reservationType(cachedState.getReservationType() == null ? null : cachedState.getReservationType().getCode())
                .queueNo(cachedState.getQueueNo())
                .duplicateRequest(Boolean.TRUE)
                .message("Duplicate request, return current state")
                .build();
    }

    /**
     * 识别当前用户在该活动上可取消的是正式报名还是候补报名。
     * <p>
     * 之所以要先解析类型，是因为正式报名与候补报名的取消逻辑、库存回补逻辑、
     * 以及落库目标表都不相同。
     * </p>
     *
     * @param activityId 活动 ID
     * @param userId 用户 ID
     * @return 可取消的预占类型
     */
    private SignupReservationTypeEnum resolveCancelableReservationType(Long activityId, Long userId) {
        ActivitySignupEntity signupEntity = activitySignupMapper.selectOne(new LambdaQueryWrapper<ActivitySignupEntity>()
                .eq(ActivitySignupEntity::getActivityId, activityId)
                .eq(ActivitySignupEntity::getUserId, userId)
                .last("limit 1"));
        if (signupEntity != null
                && (Objects.equals(signupEntity.getStatus(), SignupStatusEnum.SIGNED.getCode())
                || Objects.equals(signupEntity.getStatus(), SignupStatusEnum.PROMOTED.getCode()))) {
            return SignupReservationTypeEnum.FORMAL;
        }
        ActivityWaitlistEntity waitlistEntity = activityWaitlistMapper.selectOne(new LambdaQueryWrapper<ActivityWaitlistEntity>()
                .eq(ActivityWaitlistEntity::getActivityId, activityId)
                .eq(ActivityWaitlistEntity::getUserId, userId)
                .last("limit 1"));
        if (waitlistEntity != null && Objects.equals(waitlistEntity.getStatus(), WaitlistStatusEnum.QUEUED.getCode())) {
            return SignupReservationTypeEnum.WAITLIST;
        }
        throw new BusinessException(ErrorCode.SIGNUP_NOT_FOUND);
    }

    /**
     * 批量加载活动信息并转成 Map。
     * <p>
     * 主要用于列表查询时补充活动标题，避免对每条报名记录逐条查询活动表。
     * </p>
     *
     * @param activityIds 活动 ID 列表
     * @return 以活动 ID 为键的活动映射
     */
    private Map<Long, ActivityEntity> loadActivityMap(List<Long> activityIds) {
        if (CollectionUtils.isEmpty(activityIds)) {
            return Map.of();
        }
        return activityMapper.selectBatchIds(activityIds.stream().distinct().collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(ActivityEntity::getId, Function.identity(), (left, right) -> left));
    }

    /**
     * 安全获取活动标题。
     *
     * @param activityMap 活动映射
     * @param activityId 活动 ID
     * @return 活动标题；若活动不存在则返回 {@code null}
     */
    private String activityTitle(Map<Long, ActivityEntity> activityMap, Long activityId) {
        ActivityEntity activity = activityMap.get(activityId);
        return activity == null ? null : activity.getTitle();
    }

    /**
     * 把正式报名表状态映射成统一对外结果状态。
     *
     * @param signupStatus 正式报名表中的状态值
     * @return 统一结果状态编码
     */
    private String mapSignupResultStatus(Integer signupStatus) {
        if (Objects.equals(signupStatus, SignupStatusEnum.SIGNED.getCode())
                || Objects.equals(signupStatus, SignupStatusEnum.PROMOTED.getCode())
                || Objects.equals(signupStatus, SignupStatusEnum.COMPLETED.getCode())
                || Objects.equals(signupStatus, SignupStatusEnum.NO_SHOW.getCode())) {
            return SignupResultStatusEnum.SIGNED.getCode();
        }
        if (Objects.equals(signupStatus, SignupStatusEnum.CANCELED.getCode())) {
            return SignupResultStatusEnum.CANCELED.getCode();
        }
        return SignupResultStatusEnum.NONE.getCode();
    }

    /**
     * 把候补表状态映射成统一对外结果状态。
     *
     * @param waitlistStatus 候补表中的状态值
     * @return 统一结果状态编码
     */
    private String mapWaitlistResultStatus(Integer waitlistStatus) {
        if (Objects.equals(waitlistStatus, WaitlistStatusEnum.QUEUED.getCode())
                || Objects.equals(waitlistStatus, WaitlistStatusEnum.PROMOTED.getCode())) {
            return SignupResultStatusEnum.WAITLISTED.getCode();
        }
        if (Objects.equals(waitlistStatus, WaitlistStatusEnum.CANCELED.getCode())
                || Objects.equals(waitlistStatus, WaitlistStatusEnum.EXPIRED.getCode())) {
            return SignupResultStatusEnum.CANCELED.getCode();
        }
        return SignupResultStatusEnum.NONE.getCode();
    }

    /**
     * 根据统一结果状态返回展示文案。
     *
     * @param resultStatus 统一结果状态编码
     * @return 对应的提示文案
     */
    private String resultMessage(String resultStatus) {
        if (SignupResultStatusEnum.PROCESSING.getCode().equals(resultStatus)) {
            return "Result is still being persisted";
        }
        if (SignupResultStatusEnum.SIGNED.getCode().equals(resultStatus)) {
            return "Signup succeeded";
        }
        if (SignupResultStatusEnum.WAITLISTED.getCode().equals(resultStatus)) {
            return "Waitlist joined successfully";
        }
        if (SignupResultStatusEnum.CANCELED.getCode().equals(resultStatus)) {
            return "Signup has been canceled";
        }
        if (SignupResultStatusEnum.FAILED.getCode().equals(resultStatus)) {
            return "Signup handling failed";
        }
        return "No signup record found";
    }

    /**
     * 从登录上下文中提取当前用户 ID。
     *
     * @param loginUser 登录用户上下文
     * @return 当前用户 ID
     */
    private Long currentUserId(LoginUser loginUser) {
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return loginUser.getUserId();
    }

    /**
     * 生成一次报名请求的唯一请求号。
     * <p>
     * 请求号会贯穿 Redis 状态、Kafka 消息和结果查询，是整个报名链路的重要追踪标识。
     * </p>
     *
     * @return 不带中划线的 UUID 字符串
     */
    private String newRequestId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}

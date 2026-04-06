package com.joinup.waitlist.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.joinup.activity.entity.ActivityEntity;
import com.joinup.activity.mapper.ActivityMapper;
import com.joinup.common.context.LoginUser;
import com.joinup.common.exception.BusinessException;
import com.joinup.common.exception.ErrorCode;
import com.joinup.waitlist.domain.WaitlistProperties;
import com.joinup.waitlist.domain.WaitlistRedisQueueService;
import com.joinup.waitlist.entity.ActivityWaitlistEntity;
import com.joinup.waitlist.enums.WaitlistStatusEnum;
import com.joinup.waitlist.mapper.ActivityWaitlistMapper;
import com.joinup.waitlist.service.WaitlistService;
import com.joinup.waitlist.vo.WaitlistActivityVO;
import com.joinup.waitlist.vo.WaitlistMyItemVO;
import com.joinup.waitlist.vo.WaitlistQueueItemVO;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 候补查询服务实现。
 */
@Service
public class WaitlistServiceImpl implements WaitlistService {

    private static final int MAX_MY_RECORDS = 100;

    private final ActivityWaitlistMapper activityWaitlistMapper;
    private final ActivityMapper activityMapper;
    private final WaitlistRedisQueueService waitlistRedisQueueService;
    private final WaitlistProperties waitlistProperties;

    /**
     * 构造候补查询服务实现。
     *
     * @param activityWaitlistMapper 候补表访问器
     * @param activityMapper 活动表访问器
     * @param waitlistRedisQueueService 候补 Redis 队列服务
     * @param waitlistProperties 候补模块配置
     */
    public WaitlistServiceImpl(ActivityWaitlistMapper activityWaitlistMapper,
                               ActivityMapper activityMapper,
                               WaitlistRedisQueueService waitlistRedisQueueService,
                               WaitlistProperties waitlistProperties) {
        this.activityWaitlistMapper = activityWaitlistMapper;
        this.activityMapper = activityMapper;
        this.waitlistRedisQueueService = waitlistRedisQueueService;
        this.waitlistProperties = waitlistProperties;
    }

    /**
     * 查询某个活动当前的候补队列信息。
     *
     * @param activityId 活动 ID
     * @param loginUser 当前登录用户
     * @return 候补队列视图对象
     */
    @Override
    public WaitlistActivityVO getActivityWaitlist(Long activityId, LoginUser loginUser) {
        ActivityEntity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND);
        }
        Long userId = currentUserId(loginUser);
        ActivityWaitlistEntity currentUserWaitlist = activityWaitlistMapper.selectByActivityIdAndUserId(activityId, userId);
        List<ActivityWaitlistEntity> queuedItems = activityWaitlistMapper.selectTopByActivityIdAndStatus(
                activityId,
                WaitlistStatusEnum.QUEUED.getCode(),
                waitlistProperties.getQueueDisplayLimit());
        List<WaitlistQueueItemVO> queueItems = queuedItems.stream()
                .map(item -> WaitlistQueueItemVO.builder()
                        .userId(item.getUserId())
                        .queueNo(item.getQueueNo())
                        .joinedAt(item.getJoinedAt())
                        .build())
                .toList();
        Integer currentRank = currentUserWaitlist != null && Objects.equals(currentUserWaitlist.getStatus(), WaitlistStatusEnum.QUEUED.getCode())
                ? waitlistRedisQueueService.getRank(activityId, userId)
                : null;
        return WaitlistActivityVO.builder()
                .activityId(activityId)
                .activityTitle(activity.getTitle())
                .totalQueued(activity.getWaitlistCount())
                .currentUserStatus(currentUserWaitlist == null ? null : waitlistStatusName(currentUserWaitlist.getStatus()))
                .currentUserQueueNo(currentUserWaitlist == null ? null : currentUserWaitlist.getQueueNo())
                .currentUserRank(currentRank)
                .currentUserConfirmDeadline(currentUserWaitlist == null ? null : currentUserWaitlist.getConfirmDeadline())
                .queueItems(queueItems)
                .build();
    }

    /**
     * 查询当前用户的候补记录列表。
     *
     * @param loginUser 当前登录用户
     * @return 候补记录列表
     */
    @Override
    public List<WaitlistMyItemVO> listMyWaitlists(LoginUser loginUser) {
        Long userId = currentUserId(loginUser);
        List<ActivityWaitlistEntity> waitlists = activityWaitlistMapper.selectList(new LambdaQueryWrapper<ActivityWaitlistEntity>()
                .eq(ActivityWaitlistEntity::getUserId, userId)
                .orderByDesc(ActivityWaitlistEntity::getUpdatedAt)
                .last("limit " + MAX_MY_RECORDS));
        Map<Long, ActivityEntity> activityMap = loadActivityMap(waitlists.stream().map(ActivityWaitlistEntity::getActivityId).toList());
        List<WaitlistMyItemVO> result = new ArrayList<>();
        for (ActivityWaitlistEntity waitlist : waitlists) {
            Integer rank = Objects.equals(waitlist.getStatus(), WaitlistStatusEnum.QUEUED.getCode())
                    ? waitlistRedisQueueService.getRank(waitlist.getActivityId(), waitlist.getUserId())
                    : null;
            ActivityEntity activity = activityMap.get(waitlist.getActivityId());
            result.add(WaitlistMyItemVO.builder()
                    .activityId(waitlist.getActivityId())
                    .activityTitle(activity == null ? null : activity.getTitle())
                    .status(waitlistStatusName(waitlist.getStatus()))
                    .queueNo(waitlist.getQueueNo())
                    .currentRank(rank)
                    .joinedAt(waitlist.getJoinedAt())
                    .promotedAt(waitlist.getPromotedAt())
                    .confirmedAt(waitlist.getConfirmedAt())
                    .confirmDeadline(waitlist.getConfirmDeadline())
                    .expiredAt(waitlist.getExpiredAt())
                    .build());
        }
        result.sort(Comparator.comparing(WaitlistMyItemVO::getJoinedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        return result;
    }

    /**
     * 批量加载活动信息并转换为映射表。
     *
     * @param activityIds 活动 ID 列表
     * @return 活动映射表
     */
    private Map<Long, ActivityEntity> loadActivityMap(List<Long> activityIds) {
        if (CollectionUtils.isEmpty(activityIds)) {
            return Map.of();
        }
        return activityMapper.selectBatchIds(activityIds.stream().distinct().toList())
                .stream()
                .collect(Collectors.toMap(ActivityEntity::getId, Function.identity(), (left, right) -> left));
    }

    /**
     * 把候补状态码转换为状态名称。
     *
     * @param status 候补状态码
     * @return 状态名称
     */
    private String waitlistStatusName(Integer status) {
        WaitlistStatusEnum waitlistStatus = WaitlistStatusEnum.fromCode(status);
        return waitlistStatus == null ? null : waitlistStatus.name();
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
}

package com.joinup.activity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.joinup.activity.domain.ActivityPermissionChecker;
import com.joinup.activity.domain.ActivityStatusFlow;
import com.joinup.activity.dto.ActivityCreateRequest;
import com.joinup.activity.dto.ActivityPageQuery;
import com.joinup.activity.dto.ActivityReviewRequest;
import com.joinup.activity.dto.ActivityUpdateRequest;
import com.joinup.activity.entity.ActivityEntity;
import com.joinup.activity.entity.ActivityStatusLogEntity;
import com.joinup.activity.entity.ActivityTagEntity;
import com.joinup.activity.enums.ActivityStatusEnum;
import com.joinup.activity.enums.ActivityTagTypeEnum;
import com.joinup.activity.mapper.ActivityMapper;
import com.joinup.activity.mapper.ActivityStatusLogMapper;
import com.joinup.activity.mapper.ActivityTagMapper;
import com.joinup.activity.service.ActivityService;
import com.joinup.activity.vo.ActivityDetailVO;
import com.joinup.activity.vo.ActivityPageItemVO;
import com.joinup.activity.vo.ActivityPageVO;
import com.joinup.activity.vo.ActivityTagVO;
import com.joinup.common.context.LoginUser;
import com.joinup.common.exception.BusinessException;
import com.joinup.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 活动模块应用服务实现。
 */
@Service
public class ActivityServiceImpl implements ActivityService {

    private final ActivityMapper activityMapper;
    private final ActivityTagMapper activityTagMapper;
    private final ActivityStatusLogMapper activityStatusLogMapper;
    private final ActivityPermissionChecker permissionChecker;

    public ActivityServiceImpl(ActivityMapper activityMapper,
                               ActivityTagMapper activityTagMapper,
                               ActivityStatusLogMapper activityStatusLogMapper,
                               ActivityPermissionChecker permissionChecker) {
        this.activityMapper = activityMapper;
        this.activityTagMapper = activityTagMapper;
        this.activityStatusLogMapper = activityStatusLogMapper;
        this.permissionChecker = permissionChecker;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivityDetailVO createActivity(LoginUser loginUser, ActivityCreateRequest request) {
        permissionChecker.assertLogin(loginUser);
        // 创建和编辑走同一套参数校验，避免规则分叉。
        validateCreateOrUpdateRequest(request.getStartTime(), request.getEndTime(), request.getSignupDeadline(),
                request.getMinGroupSize(), request.getMaxParticipants(), request.getAllowWaitlist(), request.getWaitlistLimit(),
                request.getTags());

        ActivityEntity activity = new ActivityEntity();
        activity.setOrganizerId(loginUser.getUserId());
        activity.setTitle(normalizeRequired(request.getTitle()));
        activity.setCategory(normalizeRequired(request.getActivityType()));
        activity.setDescription(normalizeNullable(request.getDescription()));
        activity.setLocationText(normalizeRequired(request.getLocation()));
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        activity.setSignupDeadline(request.getSignupDeadline());
        activity.setMaxParticipants(request.getMaxParticipants());
        activity.setMinGroupSize(request.getMinGroupSize());
        activity.setCurrentParticipants(0);
        activity.setWaitlistCount(0);
        activity.setAllowWaitlist(booleanToInt(request.getAllowWaitlist()));
        activity.setWaitlistLimit(resolveWaitlistLimit(request.getAllowWaitlist(), request.getWaitlistLimit()));
        activity.setStatus(ActivityStatusFlow.initialStatus().getCode());
        activity.setViewCount(0L);
        activity.setHeatScore(0);
        activityMapper.insert(activity);

        // 标签和状态日志都放在一个事务里，确保活动创建后的快照完整。
        replaceTags(activity.getId(), request.getTags());
        insertStatusLog(activity.getId(), ActivityStatusEnum.DRAFT, ActivityStatusFlow.initialStatus(),
                "Activity created and submitted for review", loginUser.getUserId());
        return getActivityDetail(activity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivityDetailVO updateActivity(LoginUser loginUser, Long activityId, ActivityUpdateRequest request) {
        permissionChecker.assertLogin(loginUser);
        validateCreateOrUpdateRequest(request.getStartTime(), request.getEndTime(), request.getSignupDeadline(),
                request.getMinGroupSize(), request.getMaxParticipants(), request.getAllowWaitlist(), request.getWaitlistLimit(),
                request.getTags());

        ActivityEntity activity = getActivityOrThrow(activityId);
        permissionChecker.assertOrganizer(loginUser, activity);
        ActivityStatusEnum currentStatus = statusOf(activity.getStatus());
        // 只允许在“可编辑窗口”内修改，避免影响已报名用户。
        ActivityStatusFlow.assertCanEdit(currentStatus, activity.getCurrentParticipants(), activity.getWaitlistCount());
        assertCapacityCanBeUpdated(activity, request.getMaxParticipants(), request.getAllowWaitlist(), request.getWaitlistLimit());

        activity.setTitle(normalizeRequired(request.getTitle()));
        activity.setCategory(normalizeRequired(request.getActivityType()));
        activity.setDescription(normalizeNullable(request.getDescription()));
        activity.setLocationText(normalizeRequired(request.getLocation()));
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        activity.setSignupDeadline(request.getSignupDeadline());
        activity.setMaxParticipants(request.getMaxParticipants());
        activity.setMinGroupSize(request.getMinGroupSize());
        activity.setAllowWaitlist(booleanToInt(request.getAllowWaitlist()));
        activity.setWaitlistLimit(resolveWaitlistLimit(request.getAllowWaitlist(), request.getWaitlistLimit()));
        activityMapper.updateById(activity);

        replaceTags(activity.getId(), request.getTags());
        return getActivityDetail(activity.getId());
    }

    @Override
    public ActivityDetailVO getActivityDetail(Long activityId) {
        ActivityEntity activity = getActivityOrThrow(activityId);
        return buildActivityDetailVO(activity, listTagsByActivityId(activityId));
    }

    @Override
    public ActivityPageVO pageActivities(ActivityPageQuery query) {
        Page<ActivityEntity> page = new Page<>(query.getCurrent(), query.getSize());
        LambdaQueryWrapper<ActivityEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ActivityEntity::getStartTime);

        // 当前分页查询以简单组合条件为主，后续再按性能情况下沉 XML。
        if (query.getStatus() != null) {
            wrapper.eq(ActivityEntity::getStatus, query.getStatus());
        }
        if (query.getOrganizerId() != null) {
            wrapper.eq(ActivityEntity::getOrganizerId, query.getOrganizerId());
        }
        if (StringUtils.hasText(query.getActivityType())) {
            wrapper.eq(ActivityEntity::getCategory, query.getActivityType().trim());
        }
        if (query.getAllowWaitlist() != null) {
            wrapper.eq(ActivityEntity::getAllowWaitlist, booleanToInt(query.getAllowWaitlist()));
        }
        if (query.getStartTimeFrom() != null) {
            wrapper.ge(ActivityEntity::getStartTime, query.getStartTimeFrom());
        }
        if (query.getStartTimeTo() != null) {
            wrapper.le(ActivityEntity::getStartTime, query.getStartTimeTo());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(ActivityEntity::getTitle, keyword)
                    .or()
                    .like(ActivityEntity::getDescription, keyword)
                    .or()
                    .like(ActivityEntity::getLocationText, keyword));
        }

        IPage<ActivityEntity> pageResult = activityMapper.selectPage(page, wrapper);
        Map<Long, List<ActivityTagEntity>> tagMap = listTagsByActivityIds(pageResult.getRecords().stream()
                .map(ActivityEntity::getId)
                .collect(Collectors.toList()));

        List<ActivityPageItemVO> records = pageResult.getRecords().stream()
                .map(item -> buildActivityPageItemVO(item, tagMap.getOrDefault(item.getId(), Collections.emptyList())))
                .collect(Collectors.toList());

        return ActivityPageVO.builder()
                .current(pageResult.getCurrent())
                .size(pageResult.getSize())
                .total(pageResult.getTotal())
                .records(records)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivityDetailVO cancelActivity(LoginUser loginUser, Long activityId, String reason) {
        permissionChecker.assertLogin(loginUser);
        ActivityEntity activity = getActivityOrThrow(activityId);
        permissionChecker.assertOrganizer(loginUser, activity);

        ActivityStatusEnum currentStatus = statusOf(activity.getStatus());
        ActivityStatusFlow.assertCanCancel(currentStatus);
        ActivityStatusFlow.assertTransition(currentStatus, ActivityStatusEnum.CANCELED);

        activity.setStatus(ActivityStatusEnum.CANCELED.getCode());
        activity.setCancelReason(StringUtils.hasText(reason) ? reason.trim() : "Canceled by organizer");
        activityMapper.updateById(activity);

        insertStatusLog(activity.getId(), currentStatus, ActivityStatusEnum.CANCELED,
                activity.getCancelReason(), loginUser.getUserId());
        return getActivityDetail(activity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivityDetailVO reviewActivity(LoginUser loginUser, Long activityId, ActivityReviewRequest request) {
        permissionChecker.assertAdminReviewer(loginUser);
        ActivityEntity activity = getActivityOrThrow(activityId);
        ActivityStatusEnum currentStatus = statusOf(activity.getStatus());
        if (currentStatus != ActivityStatusEnum.PENDING_REVIEW) {
            throw new BusinessException(ErrorCode.ACTIVITY_STATUS_CONFLICT, "Only pending review activity can be reviewed");
        }

        ActivityStatusEnum targetStatus = ActivityStatusFlow.nextStatusOnReview(Boolean.TRUE.equals(request.getApproved()));
        ActivityStatusFlow.assertTransition(currentStatus, targetStatus);

        // reviewed_* 字段保留审核痕迹，方便后台追溯。
        activity.setStatus(targetStatus.getCode());
        activity.setReviewedBy(loginUser.getUserId());
        activity.setReviewedAt(LocalDateTime.now());
        activity.setReviewRemark(normalizeNullable(request.getRemark()));
        activityMapper.updateById(activity);

        insertStatusLog(activity.getId(), currentStatus, targetStatus,
                StringUtils.hasText(request.getRemark()) ? request.getRemark().trim() : "Activity reviewed",
                loginUser.getUserId());
        return getActivityDetail(activity.getId());
    }

    private ActivityEntity getActivityOrThrow(Long activityId) {
        ActivityEntity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND);
        }
        return activity;
    }

    private void validateCreateOrUpdateRequest(LocalDateTime startTime,
                                               LocalDateTime endTime,
                                               LocalDateTime signupDeadline,
                                               Integer minGroupSize,
                                               Integer maxParticipants,
                                               Boolean allowWaitlist,
                                               Integer waitlistLimit,
                                               List<String> tags) {
        if (startTime == null || endTime == null || signupDeadline == null) {
            throw new BusinessException(ErrorCode.ACTIVITY_INVALID_ARGUMENT);
        }
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException(ErrorCode.ACTIVITY_INVALID_ARGUMENT, "End time must be after start time");
        }
        if (!signupDeadline.isBefore(startTime)) {
            throw new BusinessException(ErrorCode.ACTIVITY_INVALID_ARGUMENT, "Signup deadline must be before start time");
        }
        if (minGroupSize == null || maxParticipants == null || minGroupSize > maxParticipants) {
            throw new BusinessException(ErrorCode.ACTIVITY_INVALID_ARGUMENT,
                    "Min group size must be less than or equal to max participants");
        }
        if (Boolean.TRUE.equals(allowWaitlist) && (waitlistLimit == null || waitlistLimit <= 0)) {
            throw new BusinessException(ErrorCode.ACTIVITY_INVALID_ARGUMENT,
                    "Waitlist limit must be positive when waitlist is enabled");
        }
        if (tags == null || tags.isEmpty()) {
            throw new BusinessException(ErrorCode.ACTIVITY_INVALID_ARGUMENT, "At least one tag is required");
        }
    }

    private void replaceTags(Long activityId, List<String> tagNames) {
        // 当前用“删后重建”简化标签同步逻辑，后续如果标签量变大再做差量更新。
        activityTagMapper.delete(new LambdaQueryWrapper<ActivityTagEntity>()
                .eq(ActivityTagEntity::getActivityId, activityId));

        Set<String> uniqueTags = new LinkedHashSet<>();
        for (String tagName : tagNames) {
            if (StringUtils.hasText(tagName)) {
                uniqueTags.add(tagName.trim());
            }
        }

        for (String tagName : uniqueTags) {
            ActivityTagEntity tagEntity = new ActivityTagEntity();
            tagEntity.setActivityId(activityId);
            tagEntity.setTagName(tagName);
            tagEntity.setTagType(ActivityTagTypeEnum.CUSTOM.getCode());
            activityTagMapper.insert(tagEntity);
        }
    }

    private void assertCapacityCanBeUpdated(ActivityEntity activity,
                                            Integer maxParticipants,
                                            Boolean allowWaitlist,
                                            Integer waitlistLimit) {
        if (activity.getCurrentParticipants() != null && maxParticipants < activity.getCurrentParticipants()) {
            throw new BusinessException(ErrorCode.ACTIVITY_INVALID_ARGUMENT,
                    "Max participants cannot be less than current participants");
        }
        if (!Boolean.TRUE.equals(allowWaitlist) && activity.getWaitlistCount() != null && activity.getWaitlistCount() > 0) {
            throw new BusinessException(ErrorCode.ACTIVITY_INVALID_ARGUMENT,
                    "Waitlist cannot be disabled when waitlist users already exist");
        }
        if (Boolean.TRUE.equals(allowWaitlist)
                && activity.getWaitlistCount() != null
                && waitlistLimit != null
                && waitlistLimit < activity.getWaitlistCount()) {
            throw new BusinessException(ErrorCode.ACTIVITY_INVALID_ARGUMENT,
                    "Waitlist limit cannot be less than current waitlist count");
        }
    }

    private void insertStatusLog(Long activityId,
                                 ActivityStatusEnum fromStatus,
                                 ActivityStatusEnum toStatus,
                                 String reason,
                                 Long operatorId) {
        // 所有显式状态变更都记录日志，后面排查流转问题会轻松很多。
        ActivityStatusLogEntity logEntity = new ActivityStatusLogEntity();
        logEntity.setActivityId(activityId);
        logEntity.setFromStatus(fromStatus.getCode());
        logEntity.setToStatus(toStatus.getCode());
        logEntity.setReason(normalizeNullable(reason));
        logEntity.setOperatorId(operatorId == null ? 0L : operatorId);
        logEntity.setOccurredAt(LocalDateTime.now());
        activityStatusLogMapper.insert(logEntity);
    }

    private List<ActivityTagEntity> listTagsByActivityId(Long activityId) {
        return activityTagMapper.selectList(new LambdaQueryWrapper<ActivityTagEntity>()
                .eq(ActivityTagEntity::getActivityId, activityId)
                .orderByAsc(ActivityTagEntity::getId));
    }

    private Map<Long, List<ActivityTagEntity>> listTagsByActivityIds(Collection<Long> activityIds) {
        if (activityIds == null || activityIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ActivityTagEntity> tags = activityTagMapper.selectList(new LambdaQueryWrapper<ActivityTagEntity>()
                .in(ActivityTagEntity::getActivityId, activityIds)
                .orderByAsc(ActivityTagEntity::getId));
        return tags.stream().collect(Collectors.groupingBy(ActivityTagEntity::getActivityId));
    }

    private ActivityDetailVO buildActivityDetailVO(ActivityEntity activity, List<ActivityTagEntity> tags) {
        return ActivityDetailVO.builder()
                .id(activity.getId())
                .organizerId(activity.getOrganizerId())
                .title(activity.getTitle())
                .activityType(activity.getCategory())
                .description(activity.getDescription())
                .location(activity.getLocationText())
                .startTime(activity.getStartTime())
                .endTime(activity.getEndTime())
                .signupDeadline(activity.getSignupDeadline())
                .maxParticipants(activity.getMaxParticipants())
                .minGroupSize(activity.getMinGroupSize())
                .currentParticipants(activity.getCurrentParticipants())
                .waitlistCount(activity.getWaitlistCount())
                .allowWaitlist(activity.getAllowWaitlist())
                .waitlistLimit(activity.getWaitlistLimit())
                .status(activity.getStatus())
                .statusDescription(statusDescription(activity.getStatus()))
                .viewCount(activity.getViewCount())
                .heatScore(activity.getHeatScore())
                .cancelReason(activity.getCancelReason())
                .reviewedBy(activity.getReviewedBy())
                .reviewedAt(activity.getReviewedAt())
                .reviewRemark(activity.getReviewRemark())
                .tags(tags.stream().map(this::buildTagVO).collect(Collectors.toList()))
                .createdAt(activity.getCreatedAt())
                .updatedAt(activity.getUpdatedAt())
                .build();
    }

    private ActivityPageItemVO buildActivityPageItemVO(ActivityEntity activity, List<ActivityTagEntity> tags) {
        return ActivityPageItemVO.builder()
                .id(activity.getId())
                .organizerId(activity.getOrganizerId())
                .title(activity.getTitle())
                .activityType(activity.getCategory())
                .location(activity.getLocationText())
                .startTime(activity.getStartTime())
                .signupDeadline(activity.getSignupDeadline())
                .maxParticipants(activity.getMaxParticipants())
                .minGroupSize(activity.getMinGroupSize())
                .currentParticipants(activity.getCurrentParticipants())
                .allowWaitlist(activity.getAllowWaitlist())
                .waitlistLimit(activity.getWaitlistLimit())
                .status(activity.getStatus())
                .statusDescription(statusDescription(activity.getStatus()))
                .viewCount(activity.getViewCount())
                .heatScore(activity.getHeatScore())
                .tags(tags.stream().map(ActivityTagEntity::getTagName).collect(Collectors.toList()))
                .build();
    }

    private ActivityTagVO buildTagVO(ActivityTagEntity tagEntity) {
        return ActivityTagVO.builder()
                .tagName(tagEntity.getTagName())
                .tagType(tagEntity.getTagType())
                .build();
    }

    private ActivityStatusEnum statusOf(Integer statusCode) {
        ActivityStatusEnum status = ActivityStatusEnum.fromCode(statusCode);
        if (status == null) {
            throw new BusinessException(ErrorCode.ACTIVITY_STATUS_CONFLICT, "Unknown activity status");
        }
        return status;
    }

    private String statusDescription(Integer statusCode) {
        ActivityStatusEnum status = ActivityStatusEnum.fromCode(statusCode);
        return status == null ? null : status.getDescription();
    }

    private Integer booleanToInt(Boolean value) {
        return Boolean.TRUE.equals(value) ? 1 : 0;
    }

    private Integer resolveWaitlistLimit(Boolean allowWaitlist, Integer waitlistLimit) {
        // 禁用候补时强制回落为 0，避免数据库里留下脏上限。
        return Boolean.TRUE.equals(allowWaitlist) ? waitlistLimit : 0;
    }

    private String normalizeRequired(String value) {
        String normalized = normalizeNullable(value);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(ErrorCode.ACTIVITY_INVALID_ARGUMENT);
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}

package com.joinup.activity.domain;

import com.joinup.activity.entity.ActivityEntity;
import com.joinup.activity.enums.ActivityStatusEnum;
import com.joinup.common.exception.BusinessException;
import com.joinup.common.exception.ErrorCode;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * 活动成团与流局领域规则。
 * <p>
 * 该类把“什么活动可以被结算”“什么时候允许结算”“按什么规则判定成团或流局”这类纯业务规则收口，
 * 避免调度器、服务层和控制层各自散落判断逻辑。
 * </p>
 */
public final class ActivitySettlementRule {

    private static final Set<ActivityStatusEnum> SETTLEMENT_CANDIDATES = EnumSet.of(
            ActivityStatusEnum.SIGNUP_OPEN,
            ActivityStatusEnum.FULL,
            ActivityStatusEnum.WAITLIST_OPEN
    );

    /**
     * 私有构造方法。
     * <p>
     * 该类是纯规则工具类，不允许被实例化。
     * </p>
     */
    private ActivitySettlementRule() {
    }

    /**
     * 判断当前活动状态是否属于可结算状态。
     *
     * @param status 活动状态枚举
     * @return 如果允许进行截止结算，则返回 {@code true}
     */
    public static boolean canSettle(ActivityStatusEnum status) {
        return status != null && SETTLEMENT_CANDIDATES.contains(status);
    }

    /**
     * 判断当前时间是否已经到达报名截止时间。
     *
     * @param signupDeadline 报名截止时间
     * @param now 当前时间
     * @return 如果已经到达或超过截止时间，则返回 {@code true}
     */
    public static boolean isDeadlineReached(LocalDateTime signupDeadline, LocalDateTime now) {
        return signupDeadline != null && now != null && !now.isBefore(signupDeadline);
    }

    /**
     * 根据活动当前正式报名人数与最少成团人数，判定结算结果。
     *
     * @param activity 活动实体
     * @return 成团成功或成团失败的目标状态
     */
    public static ActivityStatusEnum evaluateSettlementResult(ActivityEntity activity) {
        if (activity == null) {
            throw new BusinessException(ErrorCode.ACTIVITY_NOT_FOUND);
        }
        int currentParticipants = safeInt(activity.getCurrentParticipants());
        int minGroupSize = safeInt(activity.getMinGroupSize());
        return currentParticipants >= minGroupSize
                ? ActivityStatusEnum.GROUP_SUCCESS
                : ActivityStatusEnum.GROUP_FAILED;
    }

    /**
     * 把数据库中的状态码转换成活动状态枚举。
     *
     * @param statusCode 数据库状态码
     * @return 活动状态枚举
     */
    public static ActivityStatusEnum statusOf(Integer statusCode) {
        ActivityStatusEnum status = ActivityStatusEnum.fromCode(statusCode);
        if (status == null) {
            throw new BusinessException(ErrorCode.ACTIVITY_STATUS_CONFLICT, "Unknown activity status");
        }
        return status;
    }

    /**
     * 构造活动结算原因说明。
     *
     * @param targetStatus 目标状态
     * @param currentParticipants 当前正式报名人数
     * @param minGroupSize 最少成团人数
     * @param triggerReason 结算触发原因
     * @return 结算原因说明
     */
    public static String buildSettlementReason(ActivityStatusEnum targetStatus,
                                               Integer currentParticipants,
                                               Integer minGroupSize,
                                               String triggerReason) {
        String baseReason = targetStatus == ActivityStatusEnum.GROUP_SUCCESS
                ? "报名截止后满足成团人数，活动成团"
                : "报名截止后未满足成团人数，活动流局";
        return baseReason
                + "；当前正式人数=" + safeInt(currentParticipants)
                + "；最少成团人数=" + safeInt(minGroupSize)
                + (triggerReason == null || triggerReason.isBlank() ? "" : "；触发原因=" + triggerReason.trim());
    }

    /**
     * 把可空整数安全转换为非空值。
     *
     * @param value 原始整数
     * @return 非空整数；为空时返回 0
     */
    private static int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
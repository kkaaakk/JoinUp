package com.joinup.credit.domain;

import com.joinup.common.exception.BusinessException;
import com.joinup.common.exception.ErrorCode;
import com.joinup.credit.enums.CreditChangeTypeEnum;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 信用规则计算器。
 * <p>
 * 这里专门负责“怎么算”，不负责“数据从哪来、结果写到哪去”。
 * 这样 service 层只需要组织流程，而具体的规则判断可以集中在一个地方维护。
 * </p>
 */
@Component
public class CreditRuleEngine {

    private final CreditRuleProperties properties;

    /**
     * 创建信用规则计算器。
     *
     * @param properties 信用规则配置
     */
    public CreditRuleEngine(CreditRuleProperties properties) {
        this.properties = properties;
    }

    /**
     * 判断一次取消是否已经进入“临时取消”窗口。
     *
     * @param activityStartTime 活动开始时间
     * @param cancelTime 实际取消时间
     * @return 如果已经临近活动开始，则返回 {@code true}
     */
    public boolean isLateCancel(LocalDateTime activityStartTime, LocalDateTime cancelTime) {
        if (activityStartTime == null || cancelTime == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "判断取消规则时，开始时间和取消时间都不能为空");
        }
        return cancelTime.plusHours(properties.getFreeCancelHoursBeforeStart()).isAfter(activityStartTime);
    }

    /**
     * 根据取消时间计算本次取消应采用的信用变更类型。
     *
     * @param activityStartTime 活动开始时间
     * @param cancelTime 实际取消时间
     * @return 对应的信用变更类型
     */
    public CreditChangeTypeEnum resolveCancelChangeType(LocalDateTime activityStartTime, LocalDateTime cancelTime) {
        return isLateCancel(activityStartTime, cancelTime)
                ? CreditChangeTypeEnum.LATE_CANCEL_PENALTY
                : CreditChangeTypeEnum.NORMAL_CANCEL;
    }

    /**
     * 根据取消时间计算本次取消的信用分变化值。
     *
     * @param activityStartTime 活动开始时间
     * @param cancelTime 实际取消时间
     * @return 本次取消对应的信用分变化值
     */
    public Integer resolveCancelDelta(LocalDateTime activityStartTime, LocalDateTime cancelTime) {
        return isLateCancel(activityStartTime, cancelTime)
                ? properties.getLateCancelPenalty()
                : properties.getEarlyCancelPenalty();
    }

    /**
     * 判断一个活动是否属于“热门活动”。
     *
     * @param heatScore 活动热度分
     * @return 如果热度达到阈值，则返回 {@code true}
     */
    public boolean isHotActivity(Integer heatScore) {
        return heatScore != null && heatScore >= properties.getHotActivityHeatScoreThreshold();
    }

    /**
     * 把信用分压缩到合法区间。
     *
     * @param rawScore 原始计算结果
     * @return 经过上下界保护后的信用分
     */
    public Integer clampScore(Integer rawScore) {
        if (rawScore == null) {
            return properties.getInitialScore();
        }
        return Math.max(properties.getMinScore(), Math.min(properties.getMaxScore(), rawScore));
    }
}
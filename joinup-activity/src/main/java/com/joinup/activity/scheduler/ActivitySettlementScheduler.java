package com.joinup.activity.scheduler;

import com.joinup.activity.service.ActivitySettlementService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 活动报名截止结算定时任务。
 * <p>
 * 该任务定期扫描报名截止时间已到的活动，并触发成团/流局判定。
 * </p>
 */
@Component
public class ActivitySettlementScheduler {

    private final ActivitySettlementService activitySettlementService;

    /**
     * 构造活动报名截止结算定时任务。
     *
     * @param activitySettlementService 活动报名截止结算服务
     */
    public ActivitySettlementScheduler(ActivitySettlementService activitySettlementService) {
        this.activitySettlementService = activitySettlementService;
    }

    /**
     * 定时扫描并结算已到报名截止时间的活动。
     */
    @Scheduled(fixedDelayString = "${joinup.activity.settlement.scan-fixed-delay-ms:5000}")
    public void scanAndSettleActivities() {
        activitySettlementService.scanAndSettleDueActivities();
    }
}
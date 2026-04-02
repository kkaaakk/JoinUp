package com.joinup.activity.service;

/**
 * 活动报名截止结算服务接口。
 */
public interface ActivitySettlementService {

    /**
     * 扫描并结算所有已到报名截止时间的活动。
     */
    void scanAndSettleDueActivities();

    /**
     * 结算单个活动。
     *
     * @param activityId 活动 ID
     * @param operatorId 操作人 ID，系统任务可传 0
     * @param triggerReason 触发原因说明
     */
    void settleActivity(Long activityId, Long operatorId, String triggerReason);
}
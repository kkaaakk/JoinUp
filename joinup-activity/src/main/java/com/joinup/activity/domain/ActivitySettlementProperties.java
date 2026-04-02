package com.joinup.activity.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 活动结算配置属性。
 * <p>
 * 该配置类用于集中维护定时扫描批次、扫描间隔、分布式锁等待时长等可调参数。
 * </p>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "joinup.activity.settlement")
public class ActivitySettlementProperties {

    /** 每轮扫描最多结算的活动数量。 */
    private int scanBatchSize = 50;

    /** 报名截止扫描任务的固定延迟，单位毫秒。 */
    private long scanFixedDelayMs = 5000L;

    /** 获取扫描锁时的最大等待时间，单位秒。 */
    private long scanLockWaitSeconds = 0L;

    /** 扫描锁持有时间，单位秒。 */
    private long scanLockLeaseSeconds = 10L;

    /** 单个活动结算锁持有时间，单位秒。 */
    private long activityLockLeaseSeconds = 10L;
}
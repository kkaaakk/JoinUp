package com.joinup.waitlist.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 候补模块配置属性。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "joinup.waitlist")
public class WaitlistProperties {

    /** 补位确认超时时长，单位分钟。 */
    private int confirmMinutes = 15;

    /** 超时扫描任务每次处理的最大记录数。 */
    private int timeoutScanBatchSize = 50;

    /** 活动候补页默认展示的队列人数。 */
    private int queueDisplayLimit = 20;

    /** 超时扫描固定延迟，单位毫秒。 */
    private long timeoutScanFixedDelayMs = 5000L;
}

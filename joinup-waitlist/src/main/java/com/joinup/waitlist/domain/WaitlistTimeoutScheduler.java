package com.joinup.waitlist.domain;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 候补补位超时扫描任务。
 */
@Component
public class WaitlistTimeoutScheduler {

    private final WaitlistPromotionDomainService waitlistPromotionDomainService;

    /**
     * 构造超时扫描任务。
     *
     * @param waitlistPromotionDomainService 候补补位领域服务
     */
    public WaitlistTimeoutScheduler(WaitlistPromotionDomainService waitlistPromotionDomainService) {
        this.waitlistPromotionDomainService = waitlistPromotionDomainService;
    }

    /**
     * 定时扫描待确认补位中的超时记录。
     * <p>
     * 扫描间隔通过配置控制，默认保持较短固定延迟，便于及时顺延下一位候补用户。
     * </p>
     */
    @Scheduled(fixedDelayString = "${joinup.waitlist.timeout-scan-fixed-delay-ms:5000}")
    public void scanExpiredConfirmations() {
        waitlistPromotionDomainService.processExpiredConfirmations();
    }
}

package com.joinup.signup.domain;

import com.joinup.common.event.waitlist.WaitlistPromotionExpiredEvent;
import com.joinup.common.event.waitlist.WaitlistPromotionOfferedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 候补补位事件监听器。
 * <p>
 * 该监听器把候补模块发出的补位事件转成报名模块内部的席位占用与释放动作。
 * </p>
 */
@Component
public class WaitlistPromotionEventListener {

    private final WaitlistPromotionSignupCoordinator waitlistPromotionSignupCoordinator;

    /**
     * 构造候补补位事件监听器。
     *
     * @param waitlistPromotionSignupCoordinator 候补补位与报名衔接服务
     */
    public WaitlistPromotionEventListener(WaitlistPromotionSignupCoordinator waitlistPromotionSignupCoordinator) {
        this.waitlistPromotionSignupCoordinator = waitlistPromotionSignupCoordinator;
    }

    /**
     * 处理候补补位发放事件。
     *
     * @param event 候补补位发放事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPromotionOffered(WaitlistPromotionOfferedEvent event) {
        waitlistPromotionSignupCoordinator.handlePromotionOffered(event);
    }

    /**
     * 处理候补补位超时事件。
     *
     * @param event 候补补位超时事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPromotionExpired(WaitlistPromotionExpiredEvent event) {
        waitlistPromotionSignupCoordinator.handlePromotionExpired(event);
    }
}

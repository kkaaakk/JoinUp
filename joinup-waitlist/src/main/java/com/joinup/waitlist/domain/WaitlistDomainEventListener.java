package com.joinup.waitlist.domain;

import com.joinup.common.event.signup.FormalSignupCanceledEvent;
import com.joinup.common.event.waitlist.WaitlistCanceledEvent;
import com.joinup.common.event.waitlist.WaitlistJoinedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 候补模块领域事件监听器。
 * <p>
 * 所有监听方法都放在事务提交后执行，避免读取到尚未提交的数据库状态。
 * </p>
 */
@Component
public class WaitlistDomainEventListener {

    private final WaitlistPromotionDomainService waitlistPromotionDomainService;

    /**
     * 构造候补事件监听器。
     *
     * @param waitlistPromotionDomainService 候补补位领域服务
     */
    public WaitlistDomainEventListener(WaitlistPromotionDomainService waitlistPromotionDomainService) {
        this.waitlistPromotionDomainService = waitlistPromotionDomainService;
    }

    /**
     * 处理候补入队事件。
     *
     * @param event 候补入队事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWaitlistJoined(WaitlistJoinedEvent event) {
        waitlistPromotionDomainService.handleWaitlistJoined(event);
    }

    /**
     * 处理候补取消事件。
     *
     * @param event 候补取消事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWaitlistCanceled(WaitlistCanceledEvent event) {
        waitlistPromotionDomainService.handleWaitlistCanceled(event);
    }

    /**
     * 处理正式席位释放事件。
     *
     * @param event 正式席位释放事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFormalSignupCanceled(FormalSignupCanceledEvent event) {
        waitlistPromotionDomainService.handleFormalSeatReleased(event);
    }
}

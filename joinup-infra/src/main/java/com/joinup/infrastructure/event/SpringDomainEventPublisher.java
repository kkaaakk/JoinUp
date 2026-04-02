package com.joinup.infrastructure.event;

import com.joinup.common.event.DomainEvent;
import com.joinup.common.event.DomainEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 基于 Spring ApplicationEventPublisher 的领域事件发布器。
 * 当前先走单体内事件分发，后面如果需要桥接 Kafka，只改这里即可。
 */
@Component
public class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringDomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(DomainEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}

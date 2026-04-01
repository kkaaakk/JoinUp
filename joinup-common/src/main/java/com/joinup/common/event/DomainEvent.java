package com.joinup.common.event;

import java.time.LocalDateTime;

public interface DomainEvent {

    String eventId();

    String eventType();

    LocalDateTime occurredAt();
}

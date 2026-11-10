package com.raviranjan.os.event;

import org.springframework.context.ApplicationEvent;
import java.time.Instant;

public abstract class DomainEvent extends ApplicationEvent {

    private final DomainEventType eventType;
    private final String title;
    private final String message;
    private final String priority; // LOW, MEDIUM, HIGH, URGENT
    private final String relatedEntityType;
    private final String relatedEntityId;
    private final String actionUrl;
    private final Instant occurredAt;

    public DomainEvent(
            Object source,
            DomainEventType eventType,
            String title,
            String message,
            String priority,
            String relatedEntityType,
            String relatedEntityId,
            String actionUrl
    ) {
        super(source);
        this.eventType = eventType;
        this.title = title;
        this.message = message;
        this.priority = priority != null ? priority : "MEDIUM";
        this.relatedEntityType = relatedEntityType;
        this.relatedEntityId = relatedEntityId;
        this.actionUrl = actionUrl;
        this.occurredAt = Instant.now();
    }

    public DomainEventType getEventType() { return eventType; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getPriority() { return priority; }
    public String getRelatedEntityType() { return relatedEntityType; }
    public String getRelatedEntityId() { return relatedEntityId; }
    public String getActionUrl() { return actionUrl; }
    public Instant getOccurredAt() { return occurredAt; }
}

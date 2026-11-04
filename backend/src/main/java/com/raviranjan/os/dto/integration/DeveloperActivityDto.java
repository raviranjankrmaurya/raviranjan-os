package com.raviranjan.os.dto.integration;

import com.raviranjan.os.domain.enums.ActivityType;
import java.time.Instant;
import java.util.UUID;

public class DeveloperActivityDto {
    private UUID id;
    private ActivityType activityType;
    private String sourcePlatform;
    private String externalId;
    private String title;
    private String description;
    private String externalUrl;
    private String metadata;
    private Instant occurredAt;
    private String timeAgo;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public ActivityType getActivityType() { return activityType; }
    public void setActivityType(ActivityType activityType) { this.activityType = activityType; }
    public String getSourcePlatform() { return sourcePlatform; }
    public void setSourcePlatform(String sourcePlatform) { this.sourcePlatform = sourcePlatform; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getExternalUrl() { return externalUrl; }
    public void setExternalUrl(String externalUrl) { this.externalUrl = externalUrl; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public String getTimeAgo() { return timeAgo; }
    public void setTimeAgo(String timeAgo) { this.timeAgo = timeAgo; }
}

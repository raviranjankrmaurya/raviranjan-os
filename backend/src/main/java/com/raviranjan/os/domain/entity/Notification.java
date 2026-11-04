package com.raviranjan.os.domain.entity;

import com.raviranjan.os.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, length = 50)
    private String level = "INFO";

    @Column(nullable = false, length = 50)
    private String priority = "MEDIUM"; // LOW, MEDIUM, HIGH, URGENT

    @Column(nullable = false, length = 50)
    private String type = "SYSTEM";

    @Column(name = "related_entity_type", length = 100)
    private String relatedEntityType;

    @Column(name = "related_entity_id", length = 100)
    private String relatedEntityId;

    @Column(name = "action_url", length = 500)
    private String actionUrl;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "email_dispatched", nullable = false)
    private boolean emailDispatched = false;

    @Column(name = "whatsapp_dispatched", nullable = false)
    private boolean whatsappDispatched = false;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getRelatedEntityType() { return relatedEntityType; }
    public void setRelatedEntityType(String relatedEntityType) { this.relatedEntityType = relatedEntityType; }
    public String getRelatedEntityId() { return relatedEntityId; }
    public void setRelatedEntityId(String relatedEntityId) { this.relatedEntityId = relatedEntityId; }
    public String getActionUrl() { return actionUrl; }
    public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public boolean isEmailDispatched() { return emailDispatched; }
    public void setEmailDispatched(boolean emailDispatched) { this.emailDispatched = emailDispatched; }
    public boolean isWhatsappDispatched() { return whatsappDispatched; }
    public void setWhatsappDispatched(boolean whatsappDispatched) { this.whatsappDispatched = whatsappDispatched; }
}

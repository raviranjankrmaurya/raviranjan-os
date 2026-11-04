package com.raviranjan.os.dto.crm;

import com.raviranjan.os.domain.enums.InquiryStatus;

public class InquiryStatusUpdateRequest {
    private InquiryStatus status;
    private String notes;
    private String moderationStatus;
    private Boolean spam;
    private Boolean abusive;
    private String intent;
    private String moderationReason;

    public InquiryStatus getStatus() { return status; }
    public void setStatus(InquiryStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getModerationStatus() { return moderationStatus; }
    public void setModerationStatus(String moderationStatus) { this.moderationStatus = moderationStatus; }
    public Boolean getSpam() { return spam; }
    public void setSpam(Boolean spam) { this.spam = spam; }
    public Boolean getAbusive() { return abusive; }
    public void setAbusive(Boolean abusive) { this.abusive = abusive; }
    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }
    public String getModerationReason() { return moderationReason; }
    public void setModerationReason(String moderationReason) { this.moderationReason = moderationReason; }
}

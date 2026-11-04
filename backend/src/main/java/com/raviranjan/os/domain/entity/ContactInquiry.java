package com.raviranjan.os.domain.entity;

import com.raviranjan.os.common.BaseEntity;
import com.raviranjan.os.domain.enums.InquiryStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "contact_inquiries")
public class ContactInquiry extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(length = 150)
    private String company;

    @Column(length = 255)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private InquiryStatus status = InquiryStatus.NEW;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "geo_provider", length = 60)
    private String geoProvider;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "referrer", length = 512)
    private String referrer;

    @Column(name = "browser_hash", length = 128)
    private String browserHash;

    @Column(name = "message_hash", length = 64)
    private String messageHash;

    @Column(name = "disposable_email")
    private Boolean disposableEmail = Boolean.FALSE;

    @Column(name = "repeat_submission")
    private Boolean repeatSubmission = Boolean.FALSE;

    @Column(name = "repeat_count")
    private Integer repeatCount = 1;

    @Column(name = "intent", length = 60)
    private String intent = "GENUINE_INQUIRY";

    @Column(name = "is_spam")
    private Boolean spam = Boolean.FALSE;

    @Column(name = "is_abusive")
    private Boolean abusive = Boolean.FALSE;

    @Column(name = "is_threatening")
    private Boolean threatening = Boolean.FALSE;

    @Column(name = "moderation_confidence")
    private Double moderationConfidence = 0.85;

    @Column(name = "moderation_reason", columnDefinition = "TEXT")
    private String moderationReason;

    @Column(name = "recommended_action", length = 100)
    private String recommendedAction;

    @Column(name = "moderation_status", length = 50)
    private String moderationStatus = "APPROVED";

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public InquiryStatus getStatus() { return status; }
    public void setStatus(InquiryStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getGeoProvider() { return geoProvider; }
    public void setGeoProvider(String geoProvider) { this.geoProvider = geoProvider; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getReferrer() { return referrer; }
    public void setReferrer(String referrer) { this.referrer = referrer; }
    public String getBrowserHash() { return browserHash; }
    public void setBrowserHash(String browserHash) { this.browserHash = browserHash; }
    public String getMessageHash() { return messageHash; }
    public void setMessageHash(String messageHash) { this.messageHash = messageHash; }
    public boolean isDisposableEmail() { return Boolean.TRUE.equals(disposableEmail); }
    public void setDisposableEmail(Boolean disposableEmail) { this.disposableEmail = disposableEmail != null ? disposableEmail : Boolean.FALSE; }
    public boolean isRepeatSubmission() { return Boolean.TRUE.equals(repeatSubmission); }
    public void setRepeatSubmission(Boolean repeatSubmission) { this.repeatSubmission = repeatSubmission != null ? repeatSubmission : Boolean.FALSE; }
    public int getRepeatCount() { return repeatCount != null ? repeatCount : 1; }
    public void setRepeatCount(Integer repeatCount) { this.repeatCount = repeatCount != null ? repeatCount : 1; }
    public String getIntent() { return intent != null ? intent : "GENUINE_INQUIRY"; }
    public void setIntent(String intent) { this.intent = intent; }
    public boolean isSpam() { return Boolean.TRUE.equals(spam); }
    public void setSpam(Boolean spam) { this.spam = spam != null ? spam : Boolean.FALSE; }
    public boolean isAbusive() { return Boolean.TRUE.equals(abusive); }
    public void setAbusive(Boolean abusive) { this.abusive = abusive != null ? abusive : Boolean.FALSE; }
    public boolean isThreatening() { return Boolean.TRUE.equals(threatening); }
    public void setThreatening(Boolean threatening) { this.threatening = threatening != null ? threatening : Boolean.FALSE; }
    public Double getModerationConfidence() { return moderationConfidence != null ? moderationConfidence : 0.85; }
    public void setModerationConfidence(Double moderationConfidence) { this.moderationConfidence = moderationConfidence; }
    public String getModerationReason() { return moderationReason; }
    public void setModerationReason(String moderationReason) { this.moderationReason = moderationReason; }
    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
    public String getModerationStatus() { return moderationStatus != null ? moderationStatus : "APPROVED"; }
    public void setModerationStatus(String moderationStatus) { this.moderationStatus = moderationStatus; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}

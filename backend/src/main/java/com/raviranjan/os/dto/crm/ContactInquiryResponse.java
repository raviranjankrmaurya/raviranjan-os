package com.raviranjan.os.dto.crm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.raviranjan.os.domain.enums.InquiryStatus;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContactInquiryResponse {
    private UUID id;
    private String name;
    private String email;
    private String company;
    private String subject;
    private String message;
    private InquiryStatus status;
    private String notes;
    private Instant createdAt;

    // Admin-only intelligence & moderation fields (never populated on public POST /api/contact responses)
    private String ipAddress;
    private String country;
    private String region;
    private String city;
    private Double latitude;
    private Double longitude;
    private String geoProvider;
    private String userAgent;
    private String referrer;
    private String browserHash;
    private Boolean disposableEmail;
    private Boolean repeatSubmission;
    private Integer repeatCount;
    private String intent;
    private Boolean spam;
    private Boolean abusive;
    private Boolean threatening;
    private Double moderationConfidence;
    private String moderationReason;
    private String recommendedAction;
    private String moderationStatus;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
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
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
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
    public Boolean getDisposableEmail() { return disposableEmail; }
    public void setDisposableEmail(Boolean disposableEmail) { this.disposableEmail = disposableEmail; }
    public Boolean getRepeatSubmission() { return repeatSubmission; }
    public void setRepeatSubmission(Boolean repeatSubmission) { this.repeatSubmission = repeatSubmission; }
    public Integer getRepeatCount() { return repeatCount; }
    public void setRepeatCount(Integer repeatCount) { this.repeatCount = repeatCount; }
    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }
    public Boolean getSpam() { return spam; }
    public void setSpam(Boolean spam) { this.spam = spam; }
    public Boolean getAbusive() { return abusive; }
    public void setAbusive(Boolean abusive) { this.abusive = abusive; }
    public Boolean getThreatening() { return threatening; }
    public void setThreatening(Boolean threatening) { this.threatening = threatening; }
    public Double getModerationConfidence() { return moderationConfidence; }
    public void setModerationConfidence(Double moderationConfidence) { this.moderationConfidence = moderationConfidence; }
    public String getModerationReason() { return moderationReason; }
    public void setModerationReason(String moderationReason) { this.moderationReason = moderationReason; }
    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
    public String getModerationStatus() { return moderationStatus; }
    public void setModerationStatus(String moderationStatus) { this.moderationStatus = moderationStatus; }
}

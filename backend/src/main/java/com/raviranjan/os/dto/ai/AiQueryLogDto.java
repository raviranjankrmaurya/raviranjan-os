package com.raviranjan.os.dto.ai;

import java.time.Instant;
import java.util.UUID;

public class AiQueryLogDto {
    private UUID id;
    private String sessionId;
    private String userQuery;
    private int retrievedChunksCount;
    private String answer;
    private int latencyMs;
    private boolean error;
    private String errorMessage;
    private String clientIp;
    private String country;
    private String region;
    private String city;
    private String browserHash;
    private String referrer;
    private String origin;
    private String userAgent;
    private String detectedLanguage;
    private String pageUrl;
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getUserQuery() { return userQuery; }
    public void setUserQuery(String userQuery) { this.userQuery = userQuery; }
    public int getRetrievedChunksCount() { return retrievedChunksCount; }
    public void setRetrievedChunksCount(int retrievedChunksCount) { this.retrievedChunksCount = retrievedChunksCount; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public int getLatencyMs() { return latencyMs; }
    public void setLatencyMs(int latencyMs) { this.latencyMs = latencyMs; }
    public boolean isError() { return error; }
    public void setError(boolean error) { this.error = error; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getDetectedLanguage() { return detectedLanguage; }
    public void setDetectedLanguage(String detectedLanguage) { this.detectedLanguage = detectedLanguage; }
    public String getPageUrl() { return pageUrl; }
    public void setPageUrl(String pageUrl) { this.pageUrl = pageUrl; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getBrowserHash() { return browserHash; }
    public void setBrowserHash(String browserHash) { this.browserHash = browserHash; }
    public String getReferrer() { return referrer; }
    public void setReferrer(String referrer) { this.referrer = referrer; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

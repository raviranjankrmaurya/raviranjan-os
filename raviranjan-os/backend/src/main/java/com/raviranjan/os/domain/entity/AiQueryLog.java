package com.raviranjan.os.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_query_logs")
public class AiQueryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "user_query", nullable = false, columnDefinition = "TEXT")
    private String userQuery;

    @Column(name = "retrieved_chunks_count", nullable = false)
    private int retrievedChunksCount = 0;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(name = "latency_ms", nullable = false)
    private int latencyMs = 0;

    @Column(name = "is_error", nullable = false)
    private boolean error = false;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "client_ip", length = 100)
    private String clientIp;

    @Column(name = "country", length = 100)
    private String country = "India";

    @Column(name = "region", length = 100)
    private String region = "Delhi NCR / Bihar";

    @Column(name = "city", length = 100)
    private String city = "New Delhi";

    @Column(name = "browser_hash", length = 128)
    private String browserHash;

    @Column(name = "referrer", length = 512)
    private String referrer;

    @Column(name = "origin", length = 255)
    private String origin;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "detected_language", length = 50)
    private String detectedLanguage = "ENGLISH";

    @Column(name = "page_url", length = 300)
    private String pageUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

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

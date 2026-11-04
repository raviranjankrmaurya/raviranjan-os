package com.raviranjan.os.dto.writing;

import com.raviranjan.os.domain.enums.ArticleStatus;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public class ArticleResponse {
    private UUID id;
    private String slug;
    private String title;
    private String summary;
    private String content;
    private String coverImageUrl;
    private ArticleStatus status;
    private Instant publishedAt;
    private String seoTitle;
    private String seoDescription;
    private String canonicalUrl;
    private int readingTimeMinutes;
    private UUID relatedProjectId;
    private String relatedProjectTitle;
    private Set<String> tags;
    private Instant createdAt;
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
    public ArticleStatus getStatus() { return status; }
    public void setStatus(ArticleStatus status) { this.status = status; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
    public String getSeoTitle() { return seoTitle; }
    public void setSeoTitle(String seoTitle) { this.seoTitle = seoTitle; }
    public String getSeoDescription() { return seoDescription; }
    public void setSeoDescription(String seoDescription) { this.seoDescription = seoDescription; }
    public String getCanonicalUrl() { return canonicalUrl; }
    public void setCanonicalUrl(String canonicalUrl) { this.canonicalUrl = canonicalUrl; }
    public int getReadingTimeMinutes() { return readingTimeMinutes; }
    public void setReadingTimeMinutes(int readingTimeMinutes) { this.readingTimeMinutes = readingTimeMinutes; }
    public UUID getRelatedProjectId() { return relatedProjectId; }
    public void setRelatedProjectId(UUID relatedProjectId) { this.relatedProjectId = relatedProjectId; }
    public String getRelatedProjectTitle() { return relatedProjectTitle; }
    public void setRelatedProjectTitle(String relatedProjectTitle) { this.relatedProjectTitle = relatedProjectTitle; }
    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) { this.tags = tags; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

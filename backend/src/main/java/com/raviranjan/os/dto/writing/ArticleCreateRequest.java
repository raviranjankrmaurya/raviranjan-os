package com.raviranjan.os.dto.writing;

import com.raviranjan.os.domain.enums.ArticleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public class ArticleCreateRequest {
    @NotBlank(message = "Title is required")
    private String title;
    @NotBlank(message = "Slug is required")
    private String slug;
    private String summary;
    @NotBlank(message = "Content is required")
    private String content;
    private String coverImageUrl;
    @NotNull(message = "Status is required")
    private ArticleStatus status;
    private Instant publishedAt;
    private String seoTitle;
    private String seoDescription;
    private String canonicalUrl;
    private int readingTimeMinutes = 5;
    private UUID relatedProjectId;
    private Set<String> tagNames;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
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
    public Set<String> getTagNames() { return tagNames; }
    public void setTagNames(Set<String> tagNames) { this.tagNames = tagNames; }
}

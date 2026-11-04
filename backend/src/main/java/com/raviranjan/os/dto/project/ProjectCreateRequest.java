package com.raviranjan.os.dto.project;

import com.raviranjan.os.domain.enums.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ProjectCreateRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200)
    private String title;

    @Size(max = 150)
    private String slug;

    private String shortSummary;
    private String fullDescription;
    private String problemStatement;
    private String solutionOverview;
    private String systemArchitecture;
    private String category;

    private String featuredImageUrl;
    private String liveDemoUrl;
    private String githubRepoUrl;
    private String documentationUrl;
    private String seoTitle;
    private String seoDescription;
    private String architectureStyle;

    private ProjectStatus status = ProjectStatus.PUBLISHED;

    private int progress = 85;
    private boolean featured = true;
    private int displayOrder = 0;
    private LocalDate startedAt;
    private LocalDate completedAt;

    private Set<UUID> technologyIds;
    private List<String> technologies;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getShortSummary() { return shortSummary; }
    public void setShortSummary(String shortSummary) { this.shortSummary = shortSummary; }
    public String getFullDescription() { return fullDescription; }
    public void setFullDescription(String fullDescription) { this.fullDescription = fullDescription; }
    public String getProblemStatement() { return problemStatement; }
    public void setProblemStatement(String problemStatement) { this.problemStatement = problemStatement; }
    public String getSolutionOverview() { return solutionOverview; }
    public void setSolutionOverview(String solutionOverview) { this.solutionOverview = solutionOverview; }
    public String getSystemArchitecture() { return systemArchitecture; }
    public void setSystemArchitecture(String systemArchitecture) { this.systemArchitecture = systemArchitecture; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getFeaturedImageUrl() { return featuredImageUrl; }
    public void setFeaturedImageUrl(String featuredImageUrl) { this.featuredImageUrl = featuredImageUrl; }
    public String getLiveDemoUrl() { return liveDemoUrl; }
    public void setLiveDemoUrl(String liveDemoUrl) { this.liveDemoUrl = liveDemoUrl; }
    public String getGithubRepoUrl() { return githubRepoUrl; }
    public void setGithubRepoUrl(String githubRepoUrl) { this.githubRepoUrl = githubRepoUrl; }
    public String getDocumentationUrl() { return documentationUrl; }
    public void setDocumentationUrl(String documentationUrl) { this.documentationUrl = documentationUrl; }
    public String getSeoTitle() { return seoTitle; }
    public void setSeoTitle(String seoTitle) { this.seoTitle = seoTitle; }
    public String getSeoDescription() { return seoDescription; }
    public void setSeoDescription(String seoDescription) { this.seoDescription = seoDescription; }
    public String getArchitectureStyle() { return architectureStyle; }
    public void setArchitectureStyle(String architectureStyle) { this.architectureStyle = architectureStyle; }
    public ProjectStatus getStatus() { return status; }
    public void setStatus(ProjectStatus status) { this.status = status; }
    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }
    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public LocalDate getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDate startedAt) { this.startedAt = startedAt; }
    public LocalDate getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDate completedAt) { this.completedAt = completedAt; }
    public Set<UUID> getTechnologyIds() { return technologyIds; }
    public void setTechnologyIds(Set<UUID> technologyIds) { this.technologyIds = technologyIds; }
    public List<String> getTechnologies() { return technologies; }
    public void setTechnologies(List<String> technologies) { this.technologies = technologies; }
}

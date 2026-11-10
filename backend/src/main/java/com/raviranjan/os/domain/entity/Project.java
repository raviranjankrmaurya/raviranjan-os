package com.raviranjan.os.domain.entity;

import com.raviranjan.os.common.BaseEntity;
import com.raviranjan.os.domain.enums.ProjectStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "projects")
public class Project extends BaseEntity {

    @Column(nullable = false, unique = true, length = 150)
    private String slug;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "short_summary", nullable = false, columnDefinition = "TEXT")
    private String shortSummary;

    @Column(name = "full_description", nullable = false, columnDefinition = "TEXT")
    private String fullDescription;

    @Column(name = "problem_statement", columnDefinition = "TEXT")
    private String problemStatement;

    @Column(name = "solution_overview", columnDefinition = "TEXT")
    private String solutionOverview;

    @Column(name = "system_architecture", columnDefinition = "TEXT")
    private String systemArchitecture;

    @Column(name = "category", length = 100)
    private String category = "FULL_STACK";

    @Column(name = "featured_image_url", length = 500)
    private String featuredImageUrl;

    @Column(name = "live_demo_url", length = 500)
    private String liveDemoUrl;

    @Column(name = "github_repo_url", length = 500)
    private String githubRepoUrl;

    @Column(name = "documentation_url", length = 500)
    private String documentationUrl;

    @Column(name = "seo_title", length = 250)
    private String seoTitle;

    @Column(name = "seo_description", columnDefinition = "TEXT")
    private String seoDescription;

    @Column(name = "architecture_style", length = 150)
    private String architectureStyle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProjectStatus status = ProjectStatus.PUBLISHED;

    @Column(nullable = false)
    private int progress = 0;

    @Column(name = "is_featured", nullable = false)
    private boolean featured = true;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "started_at")
    private LocalDate startedAt;

    @Column(name = "completed_at")
    private LocalDate completedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "project_technologies",
        joinColumns = @JoinColumn(name = "project_id"),
        inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private Set<Skill> technologies = new HashSet<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("displayOrder ASC")
    private List<ProjectMilestone> milestones = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("displayOrder ASC")
    private List<ProjectTask> tasks = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("releasedAt DESC")
    private List<ProjectChangelog> changelogs = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("decidedAt DESC")
    private List<ProjectDecision> decisions = new ArrayList<>();

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
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
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
    public Set<Skill> getTechnologies() { return technologies; }
    public void setTechnologies(Set<Skill> technologies) { this.technologies = technologies; }
    public List<ProjectMilestone> getMilestones() { return milestones; }
    public void setMilestones(List<ProjectMilestone> milestones) { this.milestones = milestones; }
    public List<ProjectTask> getTasks() { return tasks; }
    public void setTasks(List<ProjectTask> tasks) { this.tasks = tasks; }
    public List<ProjectChangelog> getChangelogs() { return changelogs; }
    public void setChangelogs(List<ProjectChangelog> changelogs) { this.changelogs = changelogs; }
    public List<ProjectDecision> getDecisions() { return decisions; }
    public void setDecisions(List<ProjectDecision> decisions) { this.decisions = decisions; }
}

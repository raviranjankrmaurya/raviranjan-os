package com.raviranjan.os.domain.entity;

import com.raviranjan.os.common.BaseEntity;
import com.raviranjan.os.domain.enums.LabStatus;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "lab_experiments")
public class LabExperiment extends BaseEntity {

    @Column(nullable = false, unique = true, length = 150)
    private String slug;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String objective;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String technologies;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LabStatus status = LabStatus.ACTIVE;

    @Column(nullable = false)
    private int progress = 0;

    @Column(columnDefinition = "TEXT")
    private String results;

    @Column(name = "github_url", length = 500)
    private String githubUrl;

    @Column(name = "demo_url", length = 500)
    private String demoUrl;

    @Column(columnDefinition = "TEXT")
    private String screenshots;

    @Column(name = "lessons_learned", columnDefinition = "TEXT")
    private String lessonsLearned;

    @Column(name = "future_work", columnDefinition = "TEXT")
    private String futureWork;

    @Column(name = "conducted_at")
    private LocalDate conductedAt;

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getObjective() { return objective; }
    public void setObjective(String objective) { this.objective = objective; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTechnologies() { return technologies; }
    public void setTechnologies(String technologies) { this.technologies = technologies; }
    public LabStatus getStatus() { return status; }
    public void setStatus(LabStatus status) { this.status = status; }
    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }
    public String getResults() { return results; }
    public void setResults(String results) { this.results = results; }
    public String getGithubUrl() { return githubUrl; }
    public void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }
    public String getDemoUrl() { return demoUrl; }
    public void setDemoUrl(String demoUrl) { this.demoUrl = demoUrl; }
    public String getScreenshots() { return screenshots; }
    public void setScreenshots(String screenshots) { this.screenshots = screenshots; }
    public String getLessonsLearned() { return lessonsLearned; }
    public void setLessonsLearned(String lessonsLearned) { this.lessonsLearned = lessonsLearned; }
    public String getFutureWork() { return futureWork; }
    public void setFutureWork(String futureWork) { this.futureWork = futureWork; }
    public LocalDate getConductedAt() { return conductedAt; }
    public void setConductedAt(LocalDate conductedAt) { this.conductedAt = conductedAt; }
}

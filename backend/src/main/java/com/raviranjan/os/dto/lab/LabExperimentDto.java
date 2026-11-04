package com.raviranjan.os.dto.lab;

import com.raviranjan.os.domain.enums.LabStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public class LabExperimentDto {
    private UUID id;
    @NotBlank(message = "Slug is required")
    private String slug;
    @NotBlank(message = "Title is required")
    private String title;
    @NotBlank(message = "Objective is required")
    private String objective;
    @NotBlank(message = "Description is required")
    private String description;
    private String technologies;
    @NotNull(message = "Status is required")
    private LabStatus status;
    private int progress;
    private String results;
    private String githubUrl;
    private String demoUrl;
    private String screenshots;
    private String lessonsLearned;
    private String futureWork;
    private LocalDate conductedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
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

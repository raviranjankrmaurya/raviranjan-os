package com.raviranjan.os.dto.research;

import com.raviranjan.os.domain.enums.ResearchStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public class ResearchDto {
    private UUID id;
    @NotBlank(message = "Slug is required")
    private String slug;
    @NotBlank(message = "Title is required")
    private String title;
    @NotBlank(message = "Abstract is required")
    private String abstractText;
    @NotBlank(message = "Authors are required")
    private String authors;
    @NotNull(message = "Status is required")
    private ResearchStatus status;
    private String publication;
    private String doi;
    private String pdfUrl;
    private String githubUrl;
    private String datasetUrl;
    private String methodology;
    private String results;
    private String lessonsLearned;
    private LocalDate publishedDate;
    private Set<String> tags;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAbstractText() { return abstractText; }
    public void setAbstractText(String abstractText) { this.abstractText = abstractText; }
    public String getAuthors() { return authors; }
    public void setAuthors(String authors) { this.authors = authors; }
    public ResearchStatus getStatus() { return status; }
    public void setStatus(ResearchStatus status) { this.status = status; }
    public String getPublication() { return publication; }
    public void setPublication(String publication) { this.publication = publication; }
    public String getDoi() { return doi; }
    public void setDoi(String doi) { this.doi = doi; }
    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }
    public String getGithubUrl() { return githubUrl; }
    public void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }
    public String getDatasetUrl() { return datasetUrl; }
    public void setDatasetUrl(String datasetUrl) { this.datasetUrl = datasetUrl; }
    public String getMethodology() { return methodology; }
    public void setMethodology(String methodology) { this.methodology = methodology; }
    public String getResults() { return results; }
    public void setResults(String results) { this.results = results; }
    public String getLessonsLearned() { return lessonsLearned; }
    public void setLessonsLearned(String lessonsLearned) { this.lessonsLearned = lessonsLearned; }
    public LocalDate getPublishedDate() { return publishedDate; }
    public void setPublishedDate(LocalDate publishedDate) { this.publishedDate = publishedDate; }
    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) { this.tags = tags; }
}

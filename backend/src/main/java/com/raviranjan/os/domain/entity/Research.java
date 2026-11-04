package com.raviranjan.os.domain.entity;

import com.raviranjan.os.common.BaseEntity;
import com.raviranjan.os.domain.enums.ResearchStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "research")
public class Research extends BaseEntity {

    @Column(nullable = false, unique = true, length = 150)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(name = "abstract", nullable = false, columnDefinition = "TEXT")
    private String abstractText;

    @Column(nullable = false)
    private String authors;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ResearchStatus status = ResearchStatus.IN_PROGRESS;

    @Column(length = 200)
    private String publication;

    @Column(length = 150)
    private String doi;

    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;

    @Column(name = "github_url", length = 500)
    private String githubUrl;

    @Column(name = "dataset_url", length = 500)
    private String datasetUrl;

    @Column(columnDefinition = "TEXT")
    private String methodology;

    @Column(columnDefinition = "TEXT")
    private String results;

    @Column(name = "lessons_learned", columnDefinition = "TEXT")
    private String lessonsLearned;

    @Column(name = "published_date")
    private LocalDate publishedDate;

    @ManyToMany
    @JoinTable(
        name = "research_tag_mappings",
        joinColumns = @JoinColumn(name = "research_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<ResearchTag> tags = new HashSet<>();

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
    public Set<ResearchTag> getTags() { return tags; }
    public void setTags(Set<ResearchTag> tags) { this.tags = tags; }
}

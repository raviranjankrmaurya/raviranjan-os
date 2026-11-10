package com.raviranjan.os.dto.story;

import java.time.LocalDate;
import java.util.UUID;

public class ContributionStoryDto {
    private UUID id;
    private String repository;
    private String externalContributionId;
    private String title;
    private String whatIContributed;
    private String whyIContributed;
    private String problem;
    private String whatILearned;
    private String impact;
    private String technologies;
    private LocalDate contributionDate = LocalDate.now();
    private UUID relatedProjectId;
    private String prUrl;
    private String issueUrl;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getRepository() { return repository; }
    public void setRepository(String repository) { this.repository = repository; }
    public String getExternalContributionId() { return externalContributionId; }
    public void setExternalContributionId(String externalContributionId) { this.externalContributionId = externalContributionId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getWhatIContributed() { return whatIContributed; }
    public void setWhatIContributed(String whatIContributed) { this.whatIContributed = whatIContributed; }
    public String getWhyIContributed() { return whyIContributed; }
    public void setWhyIContributed(String whyIContributed) { this.whyIContributed = whyIContributed; }
    public String getProblem() { return (problem != null && !problem.isBlank()) ? problem : (whyIContributed != null && !whyIContributed.isBlank() ? whyIContributed : whatIContributed); }
    public void setProblem(String problem) { this.problem = problem; }
    public String getWhatILearned() { return whatILearned; }
    public void setWhatILearned(String whatILearned) { this.whatILearned = whatILearned; }
    public String getImpact() { return impact; }
    public void setImpact(String impact) { this.impact = impact; }
    public String getTechnologies() { return technologies; }
    public void setTechnologies(String technologies) { this.technologies = technologies; }
    public LocalDate getContributionDate() { return contributionDate != null ? contributionDate : LocalDate.now(); }
    public void setContributionDate(LocalDate contributionDate) { this.contributionDate = contributionDate; }
    public UUID getRelatedProjectId() { return relatedProjectId; }
    public void setRelatedProjectId(UUID relatedProjectId) { this.relatedProjectId = relatedProjectId; }
    public String getPrUrl() { return prUrl; }
    public void setPrUrl(String prUrl) { this.prUrl = prUrl; }
    public String getIssueUrl() { return issueUrl; }
    public void setIssueUrl(String issueUrl) { this.issueUrl = issueUrl; }
}

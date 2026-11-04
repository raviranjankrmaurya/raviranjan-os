package com.raviranjan.os.domain.entity;

import com.raviranjan.os.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "contribution_stories")
public class ContributionStory extends BaseEntity {

    @Column(nullable = false)
    private String repository;

    @Column(name = "external_contribution_id", length = 150)
    private String externalContributionId;

    @Column(nullable = false)
    private String title;

    @Column(name = "what_i_contributed", nullable = false, columnDefinition = "TEXT")
    private String whatIContributed;

    @Column(name = "why_i_contributed", columnDefinition = "TEXT")
    private String whyIContributed;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String problem;

    @Column(name = "what_i_learned", columnDefinition = "TEXT")
    private String whatILearned;

    @Column(columnDefinition = "TEXT")
    private String impact;

    @Column(length = 500)
    private String technologies;

    @Column(name = "contribution_date", nullable = false)
    private LocalDate contributionDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_project_id")
    private Project relatedProject;

    @Column(name = "pr_url", length = 500)
    private String prUrl;

    @Column(name = "issue_url", length = 500)
    private String issueUrl;

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
    public String getProblem() { return problem; }
    public void setProblem(String problem) { this.problem = problem; }
    public String getWhatILearned() { return whatILearned; }
    public void setWhatILearned(String whatILearned) { this.whatILearned = whatILearned; }
    public String getImpact() { return impact; }
    public void setImpact(String impact) { this.impact = impact; }
    public String getTechnologies() { return technologies; }
    public void setTechnologies(String technologies) { this.technologies = technologies; }
    public LocalDate getContributionDate() { return contributionDate; }
    public void setContributionDate(LocalDate contributionDate) { this.contributionDate = contributionDate; }
    public Project getRelatedProject() { return relatedProject; }
    public void setRelatedProject(Project relatedProject) { this.relatedProject = relatedProject; }
    public String getPrUrl() { return prUrl; }
    public void setPrUrl(String prUrl) { this.prUrl = prUrl; }
    public String getIssueUrl() { return issueUrl; }
    public void setIssueUrl(String issueUrl) { this.issueUrl = issueUrl; }
}

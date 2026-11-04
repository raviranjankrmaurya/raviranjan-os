package com.raviranjan.os.domain.entity;

import com.raviranjan.os.common.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "github_commits")
public class GitHubCommit extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repo_id")
    private GitHubRepository repository;

    @Column(nullable = false, unique = true, length = 100)
    private String sha;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "author_name", length = 150)
    private String authorName;

    @Column(name = "author_date", nullable = false)
    private Instant authorDate;

    @Column(name = "html_url", length = 500)
    private String htmlUrl;

    public GitHubRepository getRepository() { return repository; }
    public void setRepository(GitHubRepository repository) { this.repository = repository; }
    public String getSha() { return sha; }
    public void setSha(String sha) { this.sha = sha; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public Instant getAuthorDate() { return authorDate; }
    public void setAuthorDate(Instant authorDate) { this.authorDate = authorDate; }
    public String getHtmlUrl() { return htmlUrl; }
    public void setHtmlUrl(String htmlUrl) { this.htmlUrl = htmlUrl; }
}

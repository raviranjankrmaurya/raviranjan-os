package com.raviranjan.os.domain.entity;

import com.raviranjan.os.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "github_repositories")
public class GitHubRepository extends BaseEntity {

    @Column(name = "github_id", nullable = false, unique = true)
    private Long githubId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "full_name", nullable = false, unique = true, length = 255)
    private String fullName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "html_url", nullable = false, length = 500)
    private String htmlUrl;

    @Column(name = "is_fork")
    private boolean fork = false;

    @Column(name = "is_private")
    private boolean isPrivate = false;

    @Column(name = "stargazers_count")
    private int stargazersCount = 0;

    @Column(name = "forks_count")
    private int forksCount = 0;

    @Column(name = "primary_language", length = 50)
    private String primaryLanguage;

    @Column(columnDefinition = "TEXT")
    private String topics;

    public Long getGithubId() { return githubId; }
    public void setGithubId(Long githubId) { this.githubId = githubId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getHtmlUrl() { return htmlUrl; }
    public void setHtmlUrl(String htmlUrl) { this.htmlUrl = htmlUrl; }
    public boolean isFork() { return fork; }
    public void setFork(boolean fork) { this.fork = fork; }
    public boolean isPrivate() { return isPrivate; }
    public void setPrivate(boolean aPrivate) { isPrivate = aPrivate; }
    public int getStargazersCount() { return stargazersCount; }
    public void setStargazersCount(int stargazersCount) { this.stargazersCount = stargazersCount; }
    public int getForksCount() { return forksCount; }
    public void setForksCount(int forksCount) { this.forksCount = forksCount; }
    public String getPrimaryLanguage() { return primaryLanguage; }
    public void setPrimaryLanguage(String primaryLanguage) { this.primaryLanguage = primaryLanguage; }
    public String getTopics() { return topics; }
    public void setTopics(String topics) { this.topics = topics; }
}

package com.raviranjan.os.domain.entity;

import com.raviranjan.os.common.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "project_changelogs")
public class ProjectChangelog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "version_tag", nullable = false, length = 50)
    private String versionTag;

    @Column(name = "release_summary", nullable = false, columnDefinition = "TEXT")
    private String releaseSummary;

    @Column(name = "commit_hash", length = 100)
    private String commitHash;

    @Column(name = "released_at", nullable = false)
    private Instant releasedAt = Instant.now();

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public String getVersionTag() { return versionTag; }
    public void setVersionTag(String versionTag) { this.versionTag = versionTag; }
    public String getReleaseSummary() { return releaseSummary; }
    public void setReleaseSummary(String releaseSummary) { this.releaseSummary = releaseSummary; }
    public String getCommitHash() { return commitHash; }
    public void setCommitHash(String commitHash) { this.commitHash = commitHash; }
    public Instant getReleasedAt() { return releasedAt; }
    public void setReleasedAt(Instant releasedAt) { this.releasedAt = releasedAt; }
}

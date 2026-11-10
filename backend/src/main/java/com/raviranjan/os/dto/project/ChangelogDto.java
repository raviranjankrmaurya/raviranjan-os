package com.raviranjan.os.dto.project;

import java.time.Instant;
import java.util.UUID;

public class ChangelogDto {
    private UUID id;
    private String versionTag;
    private String releaseSummary;
    private String commitHash;
    private Instant releasedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getVersionTag() { return versionTag; }
    public void setVersionTag(String versionTag) { this.versionTag = versionTag; }
    public String getReleaseSummary() { return releaseSummary; }
    public void setReleaseSummary(String releaseSummary) { this.releaseSummary = releaseSummary; }
    public String getCommitHash() { return commitHash; }
    public void setCommitHash(String commitHash) { this.commitHash = commitHash; }
    public Instant getReleasedAt() { return releasedAt; }
    public void setReleasedAt(Instant releasedAt) { this.releasedAt = releasedAt; }
}

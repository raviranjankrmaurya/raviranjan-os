package com.raviranjan.os.repository;

import com.raviranjan.os.domain.entity.GitHubPullRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GitHubPullRequestRepository extends JpaRepository<GitHubPullRequest, UUID> {
}

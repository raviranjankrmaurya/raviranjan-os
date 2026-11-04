package com.raviranjan.os.repository;

import com.raviranjan.os.domain.entity.GitHubCommit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GitHubCommitRepository extends JpaRepository<GitHubCommit, UUID> {
    Optional<GitHubCommit> findBySha(String sha);
    boolean existsBySha(String sha);
}
